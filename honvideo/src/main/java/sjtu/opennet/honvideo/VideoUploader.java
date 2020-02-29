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
    private String videoId;                                 //Used to add virtual end chunk to datastore and cafe.
    private final String TAG = "HONVIDEO.VideoUploader";
    private long currentDuration = 0;
    private long currentIndex = 0;
    private boolean complete = false;

    public VideoUploader(String videoId, BlockingQueue<VideoUploadTask> bQueue, BlockingQueue<ChunkPublishTask> cQueue){
        videoQueue = bQueue;
        chunkQueue = cQueue;
        this.videoId = videoId;
    }

    public void shutDown(){
//        Log.w(TAG, "Video Uploader Shut Down!!");
        interrupt();
    }

    private void safelyExitPublisher(int delay){
//        Log.d(TAG, String.format("VIDEOPIPELINE: Chunk publisher end tag will be added in %s millsecends.",delay));
        try {
            Thread.sleep(delay);
        }catch(InterruptedException ie){
//            Log.e(TAG, "Unexpected thread interruption happened when uploader sleeping to end publisher.");
            ie.printStackTrace();
        }finally {
//            Log.d(TAG, String.format("VIDEOPIPELINE: index %d: Add end task to Chunk Publish Task Queue", currentIndex));
            chunkQueue.add(ChunkPublishTask.getEndTask(videoId, currentIndex));
        }
    }


    @Override
    public void run(){
//        Log.d(TAG, "VIDEOPIPELINE: Uploader start to run.");
        VideoUploadTask vTask;
        while(!complete||isInterrupted()) {
            try {
                vTask = videoQueue.take();
                if(vTask.isEnd()){
                    //Log.d(TAG, "End task received. Stop the uploader.");
                    complete = true;
                }else {
                    //Log.d(TAG, String.format("Task at %d start to execute.", currentDuration));
                    Model.VideoChunk videoChunk = vTask.upload(currentDuration, currentIndex);
//                    Log.d(TAG, String.format("VIDEOPIPELINE: Task %s with index %d at %d upload return.", videoChunk.getChunk(), currentIndex, currentDuration));
                    Log.d(TAG, String.format("Task at %d upload return.", currentDuration));
                    //Log.d(TAG, String.format("Task at %d add to chunk task queue.", currentDuration));
                    chunkQueue.add(new ChunkPublishTask(videoChunk, false));
                    Log.d(TAG, String.format("VIDEOPIPELINE: index %d, chunk %s, Chunk built and add to chunk queue.", videoChunk.getIndex(), videoChunk.getChunk()));
                    currentDuration = videoChunk.getEndTime();
                    currentIndex++;
                }

            } catch (InterruptedException ie) {
                Log.e(TAG, "Unexpected Interrupt.");
                ie.printStackTrace();
                interrupt();
            } catch(VideoExceptions.UnexpectedEndException e){
                Log.e(TAG, "Un expected end tag.");
                e.printStackTrace();
                interrupt();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        safelyExitPublisher(1000);
        Log.d(TAG, "VIDEOPIPELINE: Uploader end safely.");
    }


}

