package sjtu.opennet.honvideo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

import sjtu.opennet.hon.Handlers;
import sjtu.opennet.hon.Textile;
import sjtu.opennet.textilepb.Model;

/**
 * The interface for video uploading. This is the only class you should use in your application.
 * The functionality of VideoUploadHelper includes: <br />
 *  - Extract video meta from video.<br />
 *  - Upload video meta to thread and cafe <br />
 *  - Segment video into small .ts chunks (around 10s for each by default).<br />
 *  - Add all the video chunks to IPFS and upload them to cafe.
 *
 * @TODO
 * Let helper able to handle ipfs and textile error. Reupload the chunks if failed.<br />
 * Replace String.format with Path.resolve for path format.
 */
public class VideoUploadHelper {
    private static final String TAG = "HONVIDEO.VideoUploadHelper";
    private VideoMeta vMeta;
    private String rootPath;
    private String videoPath;
    private String chunkPath;
    private String thumbPath;
    private String m3u8Path;
    private File m3u8;
    private VideoFileListener vObserver;
    private BlockingQueue<VideoUploadTask> videoQueue;
    private VideoUploader videoUploader;

    private BlockingQueue<ChunkPublishTask> chunkQueue;
    private ChunkPublisher chunkpublisher;

    private Context context;
    private String filePath;
    private Model.Video videoPb;

    static private Bitmap bitmapFromIpfs;

    String threadId;
    M3u8Listener listObserver;

    public VideoUploadHelper(Context context, String filePath){
        this.context = context;
        this.filePath = filePath;

        vMeta = new VideoMeta(filePath);


        //setVideoPb();   //set the value of videoPb
        //Change log:
        //  call set VideoPb after segment is end.


        rootPath = FileUtil.getAppExternalPath(context,"video");
        String tmpPath = String.format("video/%s", vMeta.getHash());
        videoPath = FileUtil.getAppExternalPath(context, tmpPath);
        tmpPath = String.format("%s/chunks", tmpPath);
        chunkPath = FileUtil.getAppExternalPath(context, tmpPath);

        thumbPath = vMeta.saveThumbnail(videoPath);
        m3u8Path = String.format("%s/playlist.m3u8", videoPath);

        m3u8 = new File(m3u8Path);
        videoQueue = new LinkedBlockingQueue<>();
        chunkQueue = new PriorityBlockingQueue<>();
        videoUploader = new VideoUploader(videoQueue, chunkQueue);
        chunkpublisher = new ChunkPublisher(chunkQueue);

        vObserver = new VideoFileListener(chunkPath, vMeta.getHash(), videoQueue, chunkQueue);
        Log.d(TAG, String.format("Uploader initialize complete for video ID %s.", vMeta.getHash()));


    }

    public String getVideoId(){
        return vMeta.getHash();
    }
    /**
     * shutDownUploader is used to stop the uploader binding with this helper.
     * It will do following things:<br />
     *  - Run a new thread so the main thread would not be blocked.<br />
     *  - Sleep for 1 second to make sure that the Observer will add the last task to videoQueue.<br />
     *  - Add the end task to videoQueue to notify the VideoUploader that these are all the task.
     */
    public class shutDownUploader extends Thread{
        @Override
        public void run(){
            try {
                Thread.sleep(1000);
                videoQueue.add(VideoUploadTask.endTask());
            }catch(InterruptedException ie){
                ie.printStackTrace();
            }
        }
    }

    /**
     * segHandler is called by FFmpeg binary executor within {@link Segmenter#segment}
     */
    private ExecuteBinaryResponseHandler segHandler = new ExecuteBinaryResponseHandler(){
        @Override
        public void onSuccess(String message) {
            Log.d(TAG, String.format("FFmpeg segment success\n%s", message));

            vObserver.stopWatching();
            new shutDownUploader().start();
            //tailer.stop();
            //tailerRunning = false;
        }

        @Override
        public void onProgress(String message){
        }

        @Override
        public void onFailure(String message) {
            Log.e(TAG, "Command failure.");

            vObserver.stopWatching();
            videoUploader.shutDown();   //shut it down instead of safely exit it using shutDownUploader
        }

        @Override
        public void onStart() {
            Log.d(TAG, "FFmpeg segment start.");
            videoUploader.start();      //Do not use run!!!!
            //chunkpublisher.start();   //chunk publish has nothing to do with listener.
                                        //so we didn't make chunk publisher ffmpeg-driven.
            vObserver.startWatching();
        }

        @Override
        public void onFinish() {
            vObserver.stopWatching();
        }
    };


    /**
     * segHandler is called by FFmpeg binary executor within {@link Segmenter#segment}
     */
    private ExecuteBinaryResponseHandler segHandler2 = new ExecuteBinaryResponseHandler(){
        @Override
        public void onSuccess(String message) {
            Log.d(TAG, String.format("FFmpeg segment success\n%s", message));

            vObserver.stopWatching();
            new shutDownUploader().start();
            //tailer.stop();
            //tailerRunning = false;
        }

        @Override
        public void onProgress(String message){
        }

        @Override
        public void onFailure(String message) {
            Log.e(TAG, "Command failure.");

            vObserver.stopWatching();
            videoUploader.shutDown();   //shut it down instead of safely exit it using shutDownUploader
        }

        @Override
        public void onStart() {
            Log.d(TAG, "FFmpeg segment start with handler v2.");
            videoUploader.start();      //Do not use run!!!!
            //chunkpublisher.start();   //chunk publish has nothing to do with listener.
            //so we didn't make chunk publisher ffmpeg-driven.
            vObserver.startWatching();
        }

        @Override
        public void onFinish() {
            vObserver.stopWatching();
            setVideoPb2();
        }
    };

    private ExecuteBinaryResponseHandler segHandler3 = new ExecuteBinaryResponseHandler(){
        @Override
        public void onSuccess(String message) {
            Log.d(TAG, String.format("FFmpeg segment success\n%s", message));

            listObserver.stopWatching();
            //new shutDownUploader().start();
        }

        @Override
        public void onProgress(String message){
        }

        @Override
        public void onFailure(String message) {
            Log.e(TAG, "Command failure.");

            listObserver.stopWatching();
            //videoUploader.shutDown();   //shut it down instead of safely exit it using shutDownUploader
        }

        @Override
        public void onStart() {
            Log.d(TAG, "FFmpeg segment start with handler v2.");
            //videoUploader.start();      //Do not use run!!!!
            listObserver.startWatching();
        }

        @Override
        public void onFinish() {
            listObserver.stopWatching();
            //setVideoPb2();
        }
    };

    private Handlers.IpfsAddDataHandler posterHandler = new Handlers.IpfsAddDataHandler() {
        @Override
        public void onComplete(String path) {
            Log.d(TAG, String.format("poster ipfs path: %s", path));
            videoPb = vMeta.getPb(path);
        }

        @Override
        public void onError(Exception e) {
            e.printStackTrace();
        }
    };

    private Handlers.IpfsAddDataHandler posterHandler2 = new Handlers.IpfsAddDataHandler() {
        @Override
        public void onComplete(String path) {
            Log.d(TAG, String.format("Poster ipfs add complete. ipfs path: %s", path));
            byte[] m3u8bytes = FileUtil.readAllBytes(m3u8Path);
            if(m3u8bytes == null){
                Log.e(TAG, "Can not get content from m3u8 file. Write empty string to video meta.");
                videoPb = vMeta.getPb2(path, "");
            }else{
                Log.d(TAG, "m3u8 content got. Write it to video meta.");
                videoPb = vMeta.getPb2(path, new String(m3u8bytes));
                publishMeta2();
            }

        }

        @Override
        public void onError(Exception e) {
            e.printStackTrace();
        }
    };

    static private Handlers.DataHandler posterReceiveHandler = new Handlers.DataHandler() {
        @Override
        public void onComplete(byte[] data, String media) {
            Log.d(TAG, String.format("Received media: %s", media));
            bitmapFromIpfs = BitmapFactory.decodeByteArray(data,0,data.length);
        }

        @Override
        public void onError(Exception e) {

        }
    };


    /**
     * Segment, upload and publish video.
     */
    public void segment(){
        try {
            chunkpublisher.start(); //It was ended by upload task.
            Segmenter.segment(context, 3, filePath, m3u8Path, chunkPath, segHandler);
            //Segmenter.segment(context, 1, filePath, m3u8Path, chunkPath, segHandler);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void segment(String threadId){
        this.threadId = threadId;
        try {
            chunkpublisher.start(); //It was ended by upload task.
            Segmenter.segment(context, 3, filePath, m3u8Path, chunkPath, segHandler2);
            //Segmenter.segment(context, 1, filePath, m3u8Path, chunkPath, segHandler);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    /**
     *
     */
    public void segmentOnly(){
        try {
            Segmenter.segment(context, 3, filePath, m3u8Path, chunkPath, Segmenter.defaultHandler);
        }catch(Exception e){
            Log.e(TAG, "Unknown error when doing segment only.");
            e.printStackTrace();
        }
    }

    public void testDecodeM3u8(){
        listObserver = new M3u8Listener(m3u8Path);
        try {
            Segmenter.segment(context, 3, filePath, m3u8Path, chunkPath, segHandler3);
        }catch(Exception e){
            Log.e(TAG, "Unknown error when test m3u8 decoder.");
            e.printStackTrace();
        }

    }

    public void publishMeta(){
        try {
            Textile.instance().videos.addVideo(videoPb);
            System.out.println("================了publish");
            Textile.instance().videos.publishVideo(videoPb);
            System.out.println("================卡住了");
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void publishMeta2(){
        try {
            Textile.instance().videos.addVideo(videoPb);
            System.out.println("================了publish");
            Textile.instance().videos.publishVideo(videoPb);
            System.out.println("================卡住了");
            Textile.instance().videos.threadAddVideo(threadId,videoPb.getId());
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public static void cleanAll(Context context){
        String rootPath = FileUtil.getAppExternalPath(context, "video");
        File rmFile = new File(rootPath);
        FileUtil.deleteContents(rmFile);
    }

    /**
     * setVideoPb will add poster to ipfs and use the ipfs hash path to create video protobuf object.
     * It will called when create the new helper instance and assigned protobuf object to variable videoPb.
     */
    private void setVideoPb(){
        Textile.instance().ipfs.ipfsAddData(vMeta.getPosterByte(),true,false,posterHandler);
    }

    /**
     * setVideoPb will add poster to ipfs and use the ipfs hash path to create video protobuf object.
     * It will called when create the new helper instance and assigned protobuf object to variable videoPb.
     */
    private void setVideoPb2(){
        Textile.instance().ipfs.ipfsAddData(vMeta.getPosterByte(),true,false,posterHandler2);
    }


    public Bitmap getPoster(){
        return vMeta.getPoster();
    }

    public static void saveBitmap(Bitmap bitmapToSave, String outPath){
        try {
            File img = new File(outPath);
            OutputStream fout = new FileOutputStream(img);
            bitmapToSave.compress(Bitmap.CompressFormat.PNG, 100, fout);
            fout.flush();
            fout.close();
        }catch(FileNotFoundException fe){
            Log.e(TAG, "File not found");
            fe.printStackTrace();
        }catch(IOException ie){
            Log.e(TAG, "ioerror");
            ie.printStackTrace();
        }catch(NullPointerException ne){
            Log.e(TAG, "Bitmap is NULL.");
            ne.printStackTrace();
        }
    }

    public Model.Video getVideoPb(){
        return videoPb;
    }

    public String getVideoPath(){
        return videoPath;
    }

    public static String getVideoPathFromID(Context context, String ID){
        String tmpPath = String.format("video/%s", ID);
        return FileUtil.getAppExternalPath(context, tmpPath);
    }
}
