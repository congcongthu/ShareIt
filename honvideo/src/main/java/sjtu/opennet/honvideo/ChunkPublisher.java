package sjtu.opennet.honvideo;

import android.util.Log;

import java.util.concurrent.BlockingQueue;

import sjtu.opennet.textilepb.Model;

public class ChunkPublisher extends Thread {
    private static final String TAG = "HONVIDEO.ChunkPublisher";
    private BlockingQueue<ChunkPublishTask> vQueue;
    private boolean complete = false;

    ChunkPublisher(BlockingQueue<ChunkPublishTask> bQueue){
        vQueue = bQueue;
    }

    public void shutDown(){
        Log.w(TAG, "Chunk Publisher Shut Down!!");
        interrupt();
    }


    @Override
    public void run(){
        Log.d(TAG, "Publisher start to run.");
        ChunkPublishTask vTask;
        while(!complete) {
            try {
                vTask = vQueue.take();
                if(vTask.isEnd()){
                    complete = true;
                }else{
                    if(!vTask.process()){
                        Log.e(TAG, String.format("Publish task at duration %d failed. Try again."));
                        Thread.sleep(500);
                        vQueue.add(vTask);
                    }
                }
            } catch (InterruptedException ie) {
                Log.e(TAG, "Unexpected Interrupt.");
                ie.printStackTrace();
                interrupt();
            } catch(Exception e){
                Log.e(TAG, "Unexpected Exception when running publish thread.");
                e.printStackTrace();
            }
            //videoQueue.notify();
        }
        Log.d(TAG, "Uploader end safely.");
    }
}
