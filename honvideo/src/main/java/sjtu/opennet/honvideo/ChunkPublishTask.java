package sjtu.opennet.honvideo;


import android.util.Log;

import sjtu.opennet.hon.Textile;
import sjtu.opennet.textilepb.Model;

/**
 * Publish chunk to thread and cafe.
 * The task has its own priority where the earlier chunk has higher priority.
 * That is because we hope the receiver can play it soon.
 */
public class ChunkPublishTask implements Comparable<ChunkPublishTask> {
    private static final String TAG = "HONVIDEO.ChunkPublishTask";
    private boolean endTag;
    private int chunkStartTime;
    private Model.VideoChunk videoChunk;

    ChunkPublishTask(Model.VideoChunk videoChunk, boolean endTag){
        this.videoChunk = videoChunk;
        this.endTag = endTag;
        if(videoChunk!=null){
            chunkStartTime = videoChunk.getStartTime();
        }
    }

    public static ChunkPublishTask getEndTask(){
        return new ChunkPublishTask(null, true);
    }

    public boolean isEnd(){
        return endTag;
    }
    public int getChunkStartTime(){
        return chunkStartTime;
    }

    @Override
    public int compareTo(ChunkPublishTask another){
        // end task has the lowest priority.
        if(this.endTag){
            return 1;
        }
        if(another.endTag){
            return -1;
        }
        return this.chunkStartTime - another.chunkStartTime;
    }

    /**
     *
     * @return success or not.
     */
    public boolean process(){
        try {
            Textile.instance().videos.addVideoChunk(videoChunk);
            Textile.instance().videos.publishVideoChunk(videoChunk);
            Log.d(TAG, String.format("Chunk at time %d published.", chunkStartTime));
            return true;
        }catch(Exception e){
            Log.e(TAG, "Unexpected error when publish video chunk");
            e.printStackTrace();
            return false;
        }
    }

}
