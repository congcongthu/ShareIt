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
                int tmpSize = vQueue.size();
                Log.d(TAG, String.format("Size of chunk queue: %s", tmpSize));
                vTask = vQueue.take();
                Log.d(TAG, "New task taken.");
                if(!vTask.isEnd()){
                    Log.d(TAG, String.format("Publish task at duration %d start.", vTask.getChunkStartTime()));
                    if(!vTask.process()){
                        Log.e(TAG, String.format("Publish task at duration %d failed. Try again.", vTask.getChunkStartTime()));
                        Thread.sleep(500);
                        vQueue.add(vTask);
                    }
                }
                else{
                    Log.d(TAG,"Publish task for virtual chunk start.");
                    if(vTask.process()) {
                        complete = true;
                    }else{
                        Log.e(TAG, "Publish task for virtual chunk failed. Try again.");
                        Thread.sleep(500);
                        vQueue.add(vTask);
                    }
                }
            }catch(Exception e){
                Log.e(TAG, "Error occur");
                e.printStackTrace();
            }
        }
        Log.d(TAG, "Publisher end safely.");
    }
}
