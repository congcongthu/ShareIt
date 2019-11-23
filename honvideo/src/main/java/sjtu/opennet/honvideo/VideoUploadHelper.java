package sjtu.opennet.honvideo;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;

import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

import sjtu.opennet.hon.Handlers;
import sjtu.opennet.hon.Textile;
import sjtu.opennet.textilepb.Model;

/**
 * The interface for video uploading. This is the only class you should use in your application.
 * The functionality of VideoUploadHelper includes: <br />
 * - Extract video meta from video.<br />
 * - Upload video meta to thread and cafe <br />
 * - Segment video into small .ts chunks (around 10s for each by default).<br />
 * - Add all the video chunks to IPFS and upload them to cafe.
 *
 * @TODO Let helper able to handle ipfs and textile error. Reupload the chunks if failed.
 * @TODO Replace String.format with Path.resolve for path format.
 */
public class VideoUploadHelper {
    private static final String TAG = "HONVIDEO.VideoUploadHelper";
    final Object POSTERLOCK = new Object();
    final Object SEGLOCK = new Object();
    M3u8Listener listObserver;
    private VideoMeta vMeta;
    private String rootPath;
    private String videoPath;
    private String chunkPath;
    private String m3u8Path;
    private BlockingQueue<VideoUploadTask> videoQueue;
    private VideoUploader videoUploader;
    private BlockingQueue<ChunkPublishTask> chunkQueue;
    private ChunkPublisher chunkpublisher;
    private ExitUploader exitUploader;

    private Context context;
    private String filePath;
    private Model.Video videoPb = null;
    private String posterHash;


    /**
     * Handler of ipfsAddData.
     */
    private Handlers.IpfsAddDataHandler posterHandler = new Handlers.IpfsAddDataHandler() {
        @Override
        public void onComplete(String path) {
            synchronized (POSTERLOCK) {
                Log.d(TAG, String.format("Poster ipfs path: %s", path));
                posterHash = path;
                POSTERLOCK.notify();
            }
        }

        @Override
        public void onError(Exception e) {
            e.printStackTrace();
            POSTERLOCK.notify();
        }
    };

    /**
     * Handler of ffmpeg.execute
     */
    private ExecuteBinaryResponseHandler segHandler = new ExecuteBinaryResponseHandler() {
        @Override
        public void onSuccess(String message) {
            Log.d(TAG, String.format("FFmpeg segment success\n%s", message));
        }

        @Override
        public void onProgress(String message) {
        }

        @Override
        public void onFailure(String message) {
            Log.e(TAG, "Command failure.");

        }

        @Override
        public void onStart() {
            Log.d(TAG, "FFmpeg segment start.");
        }

        @Override
        public void onFinish() {
            listObserver.stopWatching();
            exitUploader.start();
        }
    };

    public VideoUploadHelper(Context context, String filePath) {
        this.context = context;
        this.filePath = filePath;

        vMeta = new VideoMeta(filePath);
        rootPath = FileUtil.getAppExternalPath(context, "video");
        String tmpPath = String.format("video/%s", vMeta.getHash());
        videoPath = FileUtil.getAppExternalPath(context, tmpPath);
        tmpPath = String.format("%s/chunks", tmpPath);
        chunkPath = FileUtil.getAppExternalPath(context, tmpPath);

        vMeta.saveThumbnail(videoPath);
        m3u8Path = String.format("%s/playlist.m3u8", videoPath);

        videoQueue = new LinkedBlockingQueue<>();
        chunkQueue = new PriorityBlockingQueue<>();
        videoUploader = new VideoUploader(videoQueue, chunkQueue);
        chunkpublisher = new ChunkPublisher(chunkQueue);
        exitUploader = new ExitUploader();

        listObserver = new M3u8Listener(videoPath, vMeta.getHash(), videoQueue, chunkQueue);
        Log.d(TAG, String.format("Uploader initialize complete for video ID %s.", vMeta.getHash()));
    }


    /**
     * Clean the whole upload folder.
     *
     * @param context
     */
    public static void cleanAll(Context context) {
        String rootPath = FileUtil.getAppExternalPath(context, "video");
        File rmFile = new File(rootPath);
        FileUtil.deleteContents(rmFile);
    }

    public static String getVideoPathFromID(Context context, String ID) {
        String tmpPath = String.format("video/%s", ID);
        return FileUtil.getAppExternalPath(context, tmpPath);
    }

    public String getVideoPath(){
        return filePath;
    }

    public String getVideoId() {
        return vMeta.getHash();
    }


    public void publishMeta() {
        try {
            Textile.instance().videos.addVideo(videoPb);
            Textile.instance().videos.publishVideo(videoPb);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void publishMeta(Model.Video videoPb) {
        try {
            Textile.instance().videos.addVideo(videoPb);
            Textile.instance().videos.publishVideo(videoPb);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * getVideoPb will add poster to ipfs and use the ipfs hash path to create video protobuf object.
     * <s>NOTE: <s/>This will take a bit time. So it is wise to run it in a separated thread (for the first time).
     */
    public Model.Video getVideoPb() {
        if (videoPb != null) {
            return videoPb;
        }

        //Upload poster to ipfs
        synchronized (POSTERLOCK) {
            try {
                Textile.instance().ipfs.ipfsAddData(vMeta.getPosterByte(), true, false, posterHandler);
                POSTERLOCK.wait();
            } catch (InterruptedException ie) {
                Log.e(TAG, "InterruptedException occurred when add poster to ipfs.");
                ie.printStackTrace();
            }
        }
        videoPb = vMeta.getPb(posterHash);
        return videoPb;
    }

    public Bitmap getPoster() {
        return vMeta.getPoster();
    }

    /**
     * upload is the main interface of upload helper. It will do following tasks:<br />
     * - Publish video proto to database and cafe. <br />
     * - Run ffmpeg segment command.<br />
     * - Start the chunk upload (add data to ipfs peer) and publish (publish meta to database and cafe.) threads<br />
     * - Build the video proto object.
     *
     * @return
     */
    public void upload() {
        //Publish video meta.
        getVideoPb();
        new Thread(new metaPublisher()).start();

        //Do segmentation
        try {
            videoUploader.start();
            chunkpublisher.start(); //It was ended by upload task.
            listObserver.startWatching();
            Segmenter.segment(context, 3, filePath, m3u8Path, chunkPath, segHandler);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * stop the uploader binding with this helper.
     * It will do following things:<br />
     * - Run a new thread so the main thread would not be blocked.<br />
     * - Sleep for 100ms to make sure that the Observer will add the last task to videoQueue.<br />
     * - Add the end task to videoQueue to notify the VideoUploader that these are all the task.
     */
    public class ExitUploader extends Thread {
        @Override
        public void run() {
            try {
                Log.d(TAG, "ExitUploader start.");
                Thread.sleep(100);
                videoQueue.add(VideoUploadTask.endTask());
                Log.d(TAG, "ExitUploader complete. End task added to task queue.");
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
        }
    }

    class metaPublisher implements Runnable {
        @Override
        public void run() {
            Log.d(TAG, "Meta publish thread start.");
            publishMeta();
            Log.d(TAG, "Meta publish thread end.");
        }
    }

}

