package sjtu.opennet.honvideo;

import android.util.Log;
import java.util.concurrent.BlockingQueue;

/**
 * Upload video in a async way.
 * Each video will have its own VideoUploader (In order to count duration)
 * Uploader should end when all the chunks has been uploaded.
 */
public class VideoUploader extends Thread{
    private BlockingQueue<VideoUploadTask> videoQueue;
    private BlockingQueue<ChunkPublishTask> chunkQueue;     //Used to send endTag to chunkpublisher.
    private final String TAG = "HONVIDEO.VideoUploader";
    private int currentDuration = 0;
    private boolean complete = false;
    public VideoUploader(BlockingQueue<VideoUploadTask> bQueue, BlockingQueue<ChunkPublishTask> cQueue){
        videoQueue = bQueue;
        chunkQueue = cQueue;
    }

    public void shutDown(){
        Log.w(TAG, "Video Uploader Shut Down!!");
        interrupt();
    }

    private void safelyExitPublisher(int delay){
        Log.d(TAG, String.format("Chunk publisher end tag will be added in %s secends.",delay));
        try {
            Thread.sleep(delay);
        }catch(InterruptedException ie){
            Log.e(TAG, "Unexpected thread interruption happened when uploader sleeping to end publisher.");
            ie.printStackTrace();
        }finally {
            Log.d(TAG, "Add end task to Chunk Publish Task Queue");
            chunkQueue.add(ChunkPublishTask.getEndTask());
        }
    }
    @Override
    public void run(){
        Log.d(TAG, "Uploader start to run.");
        VideoUploadTask vTask;
        int endDuration;
        while(!complete) {
            try {
                vTask = videoQueue.take();
                endDuration = vTask.upload(currentDuration);
                currentDuration = endDuration;
                if(endDuration < 0){
                    Log.d(TAG, "End Upload Task Received. End the thread.");
                    complete = true;
                }
            } catch (InterruptedException ie) {
                Log.e(TAG, "Unexpected Interrupt.");
                ie.printStackTrace();
                interrupt();
            } catch(Exception e){
                Log.e(TAG, "Unknown exception when run uploader. Interrupt thread.");
                e.printStackTrace();
                interrupt();
            }
        }
        safelyExitPublisher(30000);
        Log.d(TAG, "Uploader end safely.");
    }


}

