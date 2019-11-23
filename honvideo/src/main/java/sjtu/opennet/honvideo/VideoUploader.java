package sjtu.opennet.honvideo;

import android.util.Log;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import sjtu.opennet.textilepb.Model;

/**
 * Upload video in a async way.
 * Each video will have its own VideoUploader (In order to count duration)
 * Uploader should end when all the chunks has been uploaded.
 */
public class VideoUploader extends Thread{
    private BlockingQueue<VideoUploadTask> videoQueue;
    private BlockingQueue<ChunkPublishTask> chunkQueue;     //Used to send endTag to chunkpublisher.
    private final String TAG = "HONVIDEO.VideoUploader";
    private long currentDuration = 0;
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
        while(!complete) {
            try {
                vTask = videoQueue.take();
                if(vTask.isEnd()){
                    Log.d(TAG, "End task received. Stop the uploader.");
                    complete = true;
                }else {
                    Log.d(TAG, String.format("Task at %d start to execute.", currentDuration));
                    Model.VideoChunk videoChunk = vTask.upload(currentDuration);
                    Log.d(TAG, String.format("Task at %d upload return.", currentDuration));
                    Log.d(TAG, String.format("Task at %d add to chunk task queue.", currentDuration));
                    chunkQueue.add(new ChunkPublishTask(videoChunk, false));
                    currentDuration = videoChunk.getEndTime();
                }

            } catch (InterruptedException ie) {
                Log.e(TAG, "Unexpected Interrupt.");
                ie.printStackTrace();
                interrupt();
            } catch(VideoExceptions.UnexpectedEndException e){
                Log.e(TAG, "Un expected end tag.");
                e.printStackTrace();
                interrupt();
            }
        }
        safelyExitPublisher(1000);
        Log.d(TAG, "Uploader end safely.");
    }


}

