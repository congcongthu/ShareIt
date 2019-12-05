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
    private long chunkStartTime;
    private Model.VideoChunk videoChunk;

    ChunkPublishTask(Model.VideoChunk videoChunk, boolean endTag){
        this.videoChunk = videoChunk;
        this.endTag = endTag;
        if(videoChunk!=null){
            chunkStartTime = videoChunk.getStartTime();
        }
    }

    public static ChunkPublishTask getEndTask(String videoId, long currentIndex){
        Model.VideoChunk virtualChunk = Model.VideoChunk.newBuilder()
                .setId(videoId)
                .setChunk(VideoHandlers.chunkEndTag)
                .setAddress(VideoHandlers.chunkEndTag)
                .setStartTime(-2)
                .setEndTime(-2)
                .setIndex(currentIndex)
                .build();
        return new ChunkPublishTask(virtualChunk, true);
    }

    public boolean isEnd(){
        return endTag;
    }
    public long getChunkStartTime(){
        return chunkStartTime;
    }
    public Model.VideoChunk getChunk(){return videoChunk;}

    @Override
    public int compareTo(ChunkPublishTask another){
        // end task has the lowest priority.
        if(this.endTag){
            return 1;
        }
        if(another.endTag){
            return -1;
        }
        return (int)(this.chunkStartTime - another.chunkStartTime);
    }

    /**
     *
     * @return success or not.
     */
    public boolean process(){
        try {
            //Log.d(TAG, String.format("VIDEOPIPELINE: %s publish task start", videoChunk.getChunk()));
            Textile.instance().videos.addVideoChunk(videoChunk);
            //Log.d(TAG, String.format("VIDEOPIPELINE: %s chunk added to local database", videoChunk.getChunk()));
            Textile.instance().videos.publishVideoChunk(videoChunk);
            //Log.d(TAG, String.format("VIDEOPIPELINE: %s chunk published to cafe", videoChunk.getChunk()));
            return true;
        }catch(Exception e){
            Log.e(TAG, "Unexpected error when publish video chunk");
            e.printStackTrace();
            return false;
        }
    }

}
