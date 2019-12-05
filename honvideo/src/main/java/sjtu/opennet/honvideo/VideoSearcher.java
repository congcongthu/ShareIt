package sjtu.opennet.honvideo;

import android.util.Log;

import java.util.HashSet;
import java.util.concurrent.BlockingQueue;

import sjtu.opennet.hon.BaseTextileEventListener;
import sjtu.opennet.hon.Handlers;
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
    private VideoHandlers.ReceiveHandler receiveHandler;
    private boolean stopThread = false;

    private static final Object LOCK = new Object(); //Make sure that there is only one Searcher running at the same time.
    private final Object WAITLOCK = new Object();

    public boolean preloadOnly = false;

    private Handlers.DataHandler ipfsHandler = new Handlers.DataHandler() {
        @Override
        public void onComplete(byte[] data, String media) {
            Log.d(TAG, "VIDEOPIPELINE: preload: ipfs.dataAtPath success.");
        }

        @Override
        public void onError(Exception e) {
            Log.e(TAG, "VIDEOPIPELINE: preload: ipfs.dataAtPath throws unexpected error when preload.");
            e.printStackTrace();
        }
    };

    VideoSearcher(String videoId, HashSet<Long> receivingChunk, VideoHandlers.SearchResultHandler handler, VideoHandlers.ReceiveHandler receiveHandler){
        this.videoId = videoId;
        this.receivingChunk = receivingChunk;
        this.handler = handler;
        this.receiveHandler = receiveHandler;
        try {
            videoPb = Textile.instance().videos.getVideo(videoId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    VideoSearcher(String videoId, HashSet<Long> receivingChunk, VideoHandlers.SearchResultHandler handler, VideoHandlers.ReceiveHandler receiveHandler, long toIndex){
        this(videoId, receivingChunk, handler, receiveHandler);
        this.toIndex = toIndex;
    }

    public static VideoSearcher createPreloadSearcher(String videoId, long toIndex){
        HashSet<Long> tmpList = new HashSet<>();
        VideoSearcher searcher = new VideoSearcher(videoId, tmpList, null, null, toIndex);
        searcher.preloadOnly = true;
        return searcher;
    }

    class ChunkQueryListener extends BaseTextileEventListener {
        @Override
        public void videoChunkQueryResult(String queryId, Model.VideoChunk vchunk) {
            synchronized (WAITLOCK) {
                if (vchunk.getId().equals(videoId)) {
                    Log.d(TAG, String.format("VIDEOPIPELINE: index %d, chunk %s. Search result get.", vchunk.getIndex(), vchunk.getChunk()));
                    receivingChunk.add(vchunk.getIndex());
                    if(!preloadOnly) {
                        if (vchunk.getChunk().equals(VideoHandlers.chunkEndTag)) {
                            //Log.d(TAG, "VIDEOPIPELINE: Set stopThread to true");
                            handler.onGetAnResult(vchunk, true);
                            stopThread = true;
                        } else {
                            handler.onGetAnResult(vchunk, false);
                        }
                    }else{
                        Log.d(TAG, String.format("VIDEOPIPELINE: index %d, chunk %s. Call ipfs.dataAtPath.", vchunk.getIndex(), vchunk.getChunk()));
                        if (vchunk.getChunk().equals(VideoHandlers.chunkEndTag)) {
                            stopThread = true;
                        } else {
                            //Do ipfs dataAtPath only.
                            Textile.instance().ipfs.dataAtPath(vchunk.getAddress(), ipfsHandler);
                        }
                    }
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
                //Log.d(TAG, String.format("VIDEOPIPELINE: %d search wait for %d ms at most.", chunkIndex, waitTime));
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
                if(!preloadOnly) {
                    Log.d(TAG, String.format("VIDEOPIPELINE: %s, search thread start.", videoId));
                }else{
                    Log.d(TAG, String.format("VIDEOPIPELINE: %s, preload thread start.", videoId));
                }
                Long currentIndex = new Long(0);
                Log.d(TAG, String.format("VIDEOPIPELINE: toIndex: %d", toIndex));
                while ((toIndex < 0 || currentIndex <= toIndex) && !stopThread && !isInterrupted()){ //??????????
                    Log.d(TAG, String.format("VIDEOPIPELINE: %d, try search this index", currentIndex));
                    Model.VideoChunk v = Textile.instance().videos.getVideoChunk(videoId, currentIndex);

                    //When chunk is already in the local DB.
                    if(v != null){
                        Log.d(TAG, String.format("Chunk %d already get", currentIndex));
                        currentIndex++;
                    } else if(receivingChunk.contains(currentIndex)){
                        Log.d(TAG, String.format("Chunk %d already searched", currentIndex));
                        currentIndex++;
                    } else {
                        Log.d(TAG, String.format("Do search for chunk %d", currentIndex));
                        searchTheChunk(videoId, currentIndex, 1200);
                    }
                }
                Textile.instance().removeEventListener(searchListener);
                if(!preloadOnly) {
                    Log.d(TAG, "VIDEOPIPELINE: Searcher end safely");
                }else{
                    Log.d(TAG, "VIDEOPIPELINE: Preloader end safely");
                }
            }catch(Exception e){
                e.printStackTrace();
                Log.e(TAG, "Error occur when search video chunk");
            }
        }
    }
}