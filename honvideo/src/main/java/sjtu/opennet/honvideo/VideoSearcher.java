package sjtu.opennet.honvideo;

import android.util.Log;

import java.util.HashSet;
import java.util.concurrent.BlockingQueue;

import sjtu.opennet.hon.BaseTextileEventListener;
import sjtu.opennet.hon.Textile;
import sjtu.opennet.textilepb.Model;
import sjtu.opennet.textilepb.QueryOuterClass;


/**
 * search, create receive task
 */
public class VideoSearcher extends Thread {
    private static final String TAG = "HONVIDEO.VideoSearcher";
    private String videoId;
    private Model.Video videoPb;
    //private long fromIndex = -1;
    private long toIndex = -1;
    private HashSet<Long> receivingChunk;
    private VideoHandlers.SearchResultHandler handler;
    private boolean stopThread = false;

    private static final Object LOCK = new Object(); //Make sure that there is only one Searcher running at the same time.
    private final Object WAITLOCK = new Object();

    VideoSearcher(String videoId, HashSet<Long> receivingChunk, VideoHandlers.SearchResultHandler handler){
        this.videoId = videoId;
        this.receivingChunk = receivingChunk;
        this.handler = handler;
        try {
            videoPb = Textile.instance().videos.getVideo(videoId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    VideoSearcher(String videoId, HashSet<Long> receivingChunk, VideoHandlers.SearchResultHandler handler, long toIndex){
        this(videoId, receivingChunk, handler);
        this.toIndex = toIndex;
    }

    class ChunkQueryListener extends BaseTextileEventListener {
        @Override
        public void videoChunkQueryResult(String queryId, Model.VideoChunk vchunk) {
            //EventBus.getDefault().post(vchunk);
            synchronized (WAITLOCK) {
                if (queryId == videoId) {
                    Log.d(TAG, String.format("VIDEOPIPELINE: %s, search result received.", vchunk.getChunk()));
                    receivingChunk.add(vchunk.getIndex());
                    handler.onGetAnResult(vchunk);
                    WAITLOCK.notify();
                }
            }
        }
    }

    public void searchTheChunk(String videoid, long chunkIndex, long waitTime){
        QueryOuterClass.QueryOptions options = QueryOuterClass.QueryOptions.newBuilder()
                .setWait(1)
                .setLimit(1)
                .build();
        QueryOuterClass.VideoChunkQuery query=QueryOuterClass.VideoChunkQuery.newBuilder()
                .setStartTime(-1)
                .setEndTime(-1)
                .setIndex(chunkIndex)
                .setId(videoid).build();
        try {
            synchronized (WAITLOCK) {
                Log.d(TAG, String.format("VIDEOPIPELINE: %d search start.", chunkIndex));
                Textile.instance().videos.searchVideoChunks(query, options);
                Log.d(TAG, String.format("VIDEOPIPELINE: %d search wait for %d ms at most.", chunkIndex, waitTime));
                WAITLOCK.wait(waitTime);    //Wait for waitTime ms at most. It can be notified by getAnResult.
                Log.d(TAG, String.format("VIDEOPIPELINE: %d search time out or notified", chunkIndex));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void stopThread(){
        stopThread = true;
    }

    @Override
    public void run() {
        synchronized (LOCK) {
            try {
                ChunkQueryListener searchListener = new ChunkQueryListener();
                Textile.instance().addEventListener(searchListener);
                Log.d(TAG, String.format("VIDEOPIPELINE: %s, search thread start.", videoId));
                int currentIndex = 0;
                long videoLength = videoPb.getVideoLength();

                while ((toIndex < 0 || currentIndex <= toIndex) && !stopThread && !isInterrupted()){
                    Log.d(TAG, String.format("VIDEOPIPELINE: %d, try search this index", currentIndex));
                    Model.VideoChunk v = Textile.instance().videos.getVideoChunk(videoId, currentIndex);

                    //When chunk is already in the local DB.
                    if(v != null){
                        Log.d(TAG, String.format("Chunk %d already get", currentIndex));
                        if(v.getEndTime() >=videoLength - 200000) {   //Microsecond, equals to 200 ms
                            return;
                        }else{
                            currentIndex++;
                        }
                    } else if(receivingChunk.contains(currentIndex)){
                        Log.d(TAG, String.format("Chunk %d already searched", currentIndex));
                        currentIndex++;
                    } else {
                        Log.d(TAG, String.format("Do search for chunk %d", currentIndex));
                        searchTheChunk(videoId, currentIndex, 1200);
                    }
                }

                Textile.instance().removeEventListener(searchListener);
            }catch(Exception e){
                e.printStackTrace();
                Log.e(TAG, "Error occur when search video chunk");
            }
        }
    }
}
