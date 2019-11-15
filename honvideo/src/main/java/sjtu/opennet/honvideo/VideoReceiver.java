package sjtu.opennet.honvideo;

import android.util.Log;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;

import sjtu.opennet.hon.Textile;
import sjtu.opennet.textilepb.Model;


/**
 * @TODO stop the thread using stop flag so that we can safely stop this thread when activity is destroy. Another way to do this is add a special end task with highest priority.
 * @TODO use local DB to replace chunklist.
 */
public class VideoReceiver extends Thread{
    private final String TAG = "HONVIDEO.VideoReceiver";

    private BlockingQueue<VideoReceiveTask> vQueue;
    private String videoPath;
    private String chunkPath;
    private boolean complete = false;
    private boolean stop = false;

    private String m3u8Path;
    private String videoId;

    //private String controlLock;
    //private HashSet<int> chunk

    public VideoReceiver(BlockingQueue<VideoReceiveTask> vQueue, String videoId, String videoPath, String chunkPath){
        this.vQueue = vQueue;
        this.videoId = videoId;
        this.videoPath = videoPath;
        this.chunkPath = chunkPath;

        this.m3u8Path = String.format("%s/playList.m3u8", videoPath);

        //lockQueue = new LinkedBlockingQueue<>();
    }


    public void shutDown(){
        Log.w(TAG, "Video Receiver Shut Down!!\nNote that this is not the normal exit method.");
        interrupt();
    }

    @Override
    public void run(){

        Log.d(TAG, "Receiver start to run.");
        /**
         * lockQueue is used to blocking this thread before task complete.
         * The integer take from it means:
         *  - 1, task success
         *  - 0, task end
         *  - -1, task fail.
         */
        BlockingQueue<Integer> lockQueue = new LinkedBlockingQueue<>();
        int returnStat;
        long currentTime;
        long endTime;
        VideoReceiveTask vTask;
        while(!complete) {
            try {
                vTask = vQueue.take();

                if(vTask.isEnd()||vTask.isDestroy()){
                    Log.d(TAG, "End or destroy Task received. End the thread.");
                    complete = true;
                }else{


                    String fileName = vTask.getFileName();
                    Model.VideoChunk localChunk = Textile.instance().videos.getVideoChunk(videoId,fileName);
                    if(localChunk == null){
                        Log.d(TAG, String.format("Receive task with file %s start.", fileName));


                        //
                        currentTime = System.currentTimeMillis();
                        boolean success = vTask.process(lockQueue);
                        Log.d(TAG, String.format("Reveive task %s return", fileName));
                        returnStat = lockQueue.take();
                        endTime = System.currentTimeMillis();
                        Log.d(TAG, String.format("Receive task %s complete. Used time %d", fileName, endTime - currentTime));
                        //




                        if(returnStat == -1){
                            Log.e(TAG, String.format("Task %s fail! Add it back to task queue", fileName));
                            //Thread.sleep(1000);
                            vQueue.add(vTask);
                            Log.e(TAG, String.format("Add the failed task %s back to queue", fileName));
                        }else if(returnStat == 1){
                            Log.d(TAG, "Task success.");
                        } else if(returnStat == 0){
                            Log.e(TAG, "That should not happen.");
                        }
                    }else{
                        Log.d(TAG, String.format("Task with file %s already received.", fileName));
                    }
                }
            } catch (InterruptedException ie) {
                Log.e(TAG, "Unexpected Interrupt.");
                ie.printStackTrace();
                interrupt();
            } catch(Exception e){
                Log.e(TAG, "Unknown Exception.");
                e.printStackTrace();
            }

        }
        Log.d(TAG, "Receiver end safely.");
    }
}
