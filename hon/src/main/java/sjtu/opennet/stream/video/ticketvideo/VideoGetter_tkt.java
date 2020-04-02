package sjtu.opennet.stream.video.ticketvideo;

import android.content.Context;
import android.util.Log;

import java.util.PriorityQueue;
import java.util.concurrent.PriorityBlockingQueue;

import sjtu.opennet.hon.BaseTextileEventListener;
import sjtu.opennet.hon.Textile;
import sjtu.opennet.textilepb.Model;
import sjtu.opennet.textilepb.QueryOuterClass;

public class VideoGetter_tkt {
    private static final String TAG = "HONVIDEO.VideoGetter_tkt";

    class ChunkCompare implements Comparable<ChunkCompare>{

        long chunkCompIndex;
        Model.VideoChunk videoChunk;

        public ChunkCompare(long chunkCompIndex, Model.VideoChunk videoChunk) {
            this.chunkCompIndex = chunkCompIndex;
            this.videoChunk = videoChunk;
        }

        @Override
        public int compareTo(ChunkCompare o) {
            if(this.chunkCompIndex < o.chunkCompIndex){
                return -1;
            }else if(this.chunkCompIndex > o.chunkCompIndex){
                return 1;
            }
            return 0;
        }
    }

    String videoId;
    Context context;
    Model.Video video;
    PriorityBlockingQueue<ChunkCompare> searchResults;
    ChunkSearchListener chunkSearchListener=new ChunkSearchListener();
    boolean finishSearch=false;
    boolean finishDownload=false;
    Object SEARCHLOCK=new Object();
    long chunkIndex=0;
    SearchThread searchThread;
    DownloadThread downloadThread;

    public VideoGetter_tkt(Context context, String videoId) {
        this.context=context;
        this.videoId = videoId;
        Log.d(TAG, "VideoGetter_tkt: get the video: "+videoId);

        try {
            video= Textile.instance().videos.getVideo(videoId);
        } catch (Exception e) {
            e.printStackTrace();
        }

        searchResults=new PriorityBlockingQueue<>();
        searchThread=new SearchThread();
        downloadThread=new DownloadThread();
    }

    public void startGet(){
        Textile.instance().addEventListener(chunkSearchListener);

        searchThread.start();
        downloadThread.start();
    }

    public void stopGet(){
        Textile.instance().removeEventListener(chunkSearchListener);
        finishSearch=true;
    }

    // search video chunk
    class SearchThread extends Thread{
        @Override
        public void run() {
            synchronized (SEARCHLOCK){
                while(!finishSearch){
                    Log.d(TAG, "run: will search : "+chunkIndex);
                    searchTheChunk(videoId,chunkIndex);
                    try {
                        Log.d(TAG, "run: wait search: "+chunkIndex);
                        SEARCHLOCK.wait(1200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    public void searchTheChunk(String videoid, long chunkIndex){
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
            Textile.instance().videos.searchVideoChunks(query, options);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    class ChunkSearchListener extends BaseTextileEventListener{
        @Override
        public void videoChunkQueryResult(String queryId, Model.VideoChunk vchunk) {
            Log.d(TAG, "videoChunkQueryResult: get search result: "+vchunk.getIndex()+" "+vchunk.getChunk());
            synchronized (SEARCHLOCK){
                Log.d(TAG, "videoChunkQueryResult, in lock: "+vchunk.getIndex()+" "+vchunk.getChunk()+" "+vchunk.getAddress());
                searchResults.add(new ChunkCompare(vchunk.getIndex(),vchunk));
                if(vchunk.getChunk().equals("VIRTUAL")){
                    finishSearch=true;
                }
                chunkIndex++;
                SEARCHLOCK.notify();
            }
        }
    }

    // download video chunk
    class DownloadThread extends Thread{
        @Override
        public void run() {
            while(!finishDownload){
                try {
                    ChunkCompare chunkCompare=searchResults.take();
                    Log.d(TAG, "run: get the compare: "+chunkCompare.chunkCompIndex);
                    if(chunkCompare.videoChunk.getChunk().equals("VIRTUAL")){
                        finishDownload=true;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Log.d(TAG, "run: finish downloading");
        }
    }
}
