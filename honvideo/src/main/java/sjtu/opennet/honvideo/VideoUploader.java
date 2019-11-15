package sjtu.opennet.honvideo;

import android.util.Log;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

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
        BlockingQueue<Integer> blockQueue = new LinkedBlockingQueue<>();
        VideoUploadTask vTask;
        int endDuration;
        int returnStat;
        long currentTime;
        long endTime;
        while(!complete) {
            try {
                vTask = videoQueue.take();



                Log.d(TAG, String.format("Task %s start to execute.", vTask.getChunkName()));
                currentTime = System.currentTimeMillis();
                endDuration = vTask.upload(currentDuration, blockQueue);
                Log.d(TAG, String.format("Task %s upload return.", vTask.getChunkName()));
                returnStat = blockQueue.take();



                endTime = System.currentTimeMillis();
                currentDuration = endDuration;
                if(endDuration < 0){
                    Log.d(TAG, "End Upload Task Received. End the thread.");
                    complete = true;
                }
                Log.d(TAG, String.format("Task %s upload and ipfs add complete. Task use time %d.", vTask.getChunkName(), endTime - currentTime));
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
        safelyExitPublisher(1000);
        Log.d(TAG, "Uploader end safely.");
    }


}

