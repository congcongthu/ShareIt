package sjtu.opennet.honvideo;

import android.util.Log;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

public class VideoReceiver extends Thread{
    private final String TAG = "HONVIDEO.VideoReceiver";

    private BlockingQueue<VideoReceiveTask> vQueue;
    private String videoPath;
    private String chunkPath;
    private boolean complete = false;


    private String m3u8Path;
    private String chunkListPath;
    private Set<Integer> chunkIndexSet;
    private int currentIndex = 0;
    public VideoReceiver(BlockingQueue<VideoReceiveTask> vQueue, String videoPath, String chunkPath){
        this.vQueue = vQueue;
        this.videoPath = videoPath;
        this.chunkPath = chunkPath;

        this.m3u8Path = String.format(videoPath, "playList.m3u8");
        this.chunkListPath = String.format(videoPath, "chunkList");

        chunkIndexSet = new HashSet<>();

    }

    /**
     * resume state from chunklist and m3u8
     */
    private void resumeState(){

    }

    /**
     * updateState does following things:<br />
     *  - Add chunk index to chunk index list
     *  - Update currentIndex if the index if successor of currentIndex
     *  - Write to m3u8 file if currentIndex is updated.
     *  - Further update currentIndex and m3u8 if there is more successors in chunk index list.
     */
    private void updateState(){


    }

    public void shutDown(){
        Log.w(TAG, "Video Receiver Shut Down!!\nNote that this is not the normal exit method.");
        interrupt();
    }

    @Override
    public void run(){
        Log.d(TAG, "Receiver start to run.");
        VideoReceiveTask vTask;
        while(!complete) {
            try {
                vTask = vQueue.take();
                if(vTask.isEnd()){
                    Log.d(TAG, "End Task received. End the thread.");
                    complete = true;
                }else{
                    //
                    //DO SOMETHING
                    //Judeg whether to process task
                    String fileName = vTask.getFileName();
                    //String fileName = vTask.process();
                    int tmpIndex = Segmenter.getIndFromPath(fileName);
                    if(chunkIndexSet.contains(tmpIndex)){
                        Log.d(TAG, String.format("File %s already received.", fileName));
                    }else{
                        Log.d(TAG, String.format("Task with file %s start.", fileName));
                        boolean success = vTask.process();
                        if(!success){
                            Thread.sleep(1000);
                            vQueue.add(vTask);
                            Log.e(TAG, String.format("Add the failed task %s back to queue", fileName));
                        }
                    }

                    //
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
        Log.d(TAG, "Receiver end safely.");
    }
}
