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
    static final Object SEGTHREADLOCK = new Object();
    final Object SEGLOCK = new Object();
    //final Object SEGLOCK = new Object();
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

    private boolean cafeStore;
    private String command;

    private final int segmentTime = 3;

    /**
     * Handler of ipfsAddData.
     */
    private Handlers.IpfsAddDataHandler posterHandler = new Handlers.IpfsAddDataHandler() {
        @Override
        public void onComplete(String path) {
            synchronized (POSTERLOCK) {
                //Log.d(TAG, String.format("Poster ipfs path: %s", path));
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
            Log.e(TAG, "FFmpeg command failure.");

        }

        @Override
        public void onStart() {
            Log.d(TAG, "FFmpeg segment start.");
        }

        @Override
        public void onFinish() {
            synchronized (SEGLOCK) {
                listObserver.stopWatching();
                exitUploader.start();
                Log.d(TAG, "FFmpeg segment finish.");
                Log.d(TAG, "SEGLOCK NOTIFY");
                SEGLOCK.notify();

            }
        }
    };

    private VideoHandlers.UploadHandler uploadHandler;

    public VideoUploadHelper(Context context, String filePath, boolean cafeStore) {
        this.context = context;
        this.filePath = filePath;
        this.cafeStore=cafeStore;

        this.command = String.format("-i %s -c copy -bsf:v h264_mp4toannexb -map 0 -f segment " +
                "-segment_time %d " +
                "-segment_list_size 1 " +
                "-segment_list %s " +
                "%s/out%%04d.ts", filePath,segmentTime, m3u8Path, chunkPath);
        vMeta = new VideoMeta(filePath, command.getBytes());

        rootPath = FileUtil.getAppExternalPath(context, "video");
        String tmpPath = String.format("video/%s", vMeta.getHash());
        videoPath = FileUtil.getAppExternalPath(context, tmpPath);
        tmpPath = String.format("%s/chunks", tmpPath);
        chunkPath = FileUtil.getAppExternalPath(context, tmpPath);

        vMeta.saveThumbnail(videoPath);
        m3u8Path = String.format("%s/playlist.m3u8", videoPath);

        videoQueue = new LinkedBlockingQueue<>();
        chunkQueue = new PriorityBlockingQueue<>();
        videoUploader = new VideoUploader(vMeta.getHash(), videoQueue, chunkQueue);
        chunkpublisher = new ChunkPublisher(chunkQueue);
        exitUploader = new ExitUploader();

        listObserver = new M3u8Listener(videoPath, vMeta.getHash(), videoQueue, chunkQueue);
        Log.d(TAG, String.format("Uploader initialize complete for video ID %s.", vMeta.getHash()));
    }

    public void logCatVideoMeta(){
        vMeta.logCatPrint();
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

//            Textile.instance().videos.publishVideo(videoPb, cafeStore);

//            Log.d(TAG, "publishMeta: publish successly");
            uploadHandler.onPublishComplete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void publishMeta(Model.Video videoPb) {
        try {
            Textile.instance().videos.addVideo(videoPb);
            Textile.instance().videos.publishVideo(videoPb, true);
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
    public void upload(VideoHandlers.UploadHandler uploadHandler) {
        this.uploadHandler=uploadHandler;

        //Publish video meta.
        getVideoPb();
        new Thread(new metaPublisher()).start();

        //Do segmentation
        try {
            videoUploader.start();
            chunkpublisher.start(); //It was ended by upload task.
            listObserver.startWatching();
            //Segmenter.segment(context, 1, filePath, m3u8Path, chunkPath, segHandler);
            new segmentThread().start();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    class segmentThread extends Thread {
//        private Context context;
//        int segTime;
//        String filePath;
//        String m3u8Path;
//        String chunkPath;
//        ExecuteBinaryResponseHandler segHandler;
//        segmentThread(Context context, int segTime, String m3u8Path, String chunkPath, ExecuteBinaryResponseHandler segHandler){
//            this.context = context;
//            this.segTime = segTime;
//            this.m3u8Path = m3u8Path;
//            this.chunkPath = chunkPath;
//            this.segHandler = segHandler;
//        }

        @Override
        public void run(){
            synchronized (SEGTHREADLOCK) {
                synchronized (SEGLOCK) {
                    try {
                        File outDirf = new File(chunkPath);
                        if(!outDirf.exists()){
                            outDirf.mkdir();
                        }
                        Segmenter.segment(context, command, segHandler);
//                        Log.d(TAG, "SEGLOCK WAIT");
                        SEGLOCK.wait();
//                        Log.d(TAG, "SEGLOCK NOTIFUED");
                    } catch (Exception e) {
                        Log.e(TAG, "Error occur when segment video.");
                        e.printStackTrace();
                    }
                }
            }
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
//            Log.d(TAG, "VIDEOPIPELINE: Meta publish thread start.");
            publishMeta();
//            Log.d(TAG, "VIDEOPIPELINE: Meta publish thread end.");
        }
    }

}

