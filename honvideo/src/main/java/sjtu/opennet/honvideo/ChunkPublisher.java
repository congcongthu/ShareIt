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
                //Log.d(TAG, "New task taken.");
                if(!vTask.isEnd()){
                    Log.d(TAG, String.format("VIDEOPIPELINE: index %d, chunk %s. Publish task start.", vTask.getChunk().getIndex(), vTask.getChunk().getChunk()));
                    if(!vTask.process()){
                        Log.e(TAG, String.format("VIDEOPIPELINE: index %d, chunk %s. Publish failed. Try again.", vTask.getChunk().getIndex(), vTask.getChunk().getChunk()));
                        Thread.sleep(500);
                        vQueue.add(vTask);
                    }
                }
                else{
                    Log.d(TAG, String.format("VIDEOPIPELINE: index %d, chunk %s. Publish task start.", vTask.getChunk().getIndex(), vTask.getChunk().getChunk()));
                    if(vTask.process()) {
                        complete = true;
                    }else{
                        Log.e(TAG, String.format("VIDEOPIPELINE: index %d, chunk %s. Publish failed. Try again.", vTask.getChunk().getIndex(), vTask.getChunk().getChunk()));
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
