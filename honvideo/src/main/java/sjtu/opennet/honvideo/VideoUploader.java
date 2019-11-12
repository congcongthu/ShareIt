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
    private final String TAG = "HONVIDEO.VideoUploader";
    private int currentDuration = 0;
    private boolean complete = false;
    public VideoUploader(BlockingQueue<VideoUploadTask> bQueue){
        videoQueue = bQueue;
    }

    public void shutDown(){
        Log.w(TAG, "Video Uploader Shut Down!!");
        interrupt();
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
                    Log.d(TAG, "End Task received. End the thread.");
                    complete = true;
                }
            } catch (InterruptedException ie) {
                Log.e(TAG, "Unexpected Interrupt.");
                ie.printStackTrace();
                interrupt();
            } catch(Exception e){
                e.printStackTrace();
            }
            //videoQueue.notify();
        }
        Log.d(TAG, "Uploader end safely.");
    }


}

