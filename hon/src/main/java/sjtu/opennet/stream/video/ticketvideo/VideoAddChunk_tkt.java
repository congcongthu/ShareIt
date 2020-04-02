package sjtu.opennet.stream.video.ticketvideo;

import android.os.FileObserver;
import android.support.annotation.Nullable;
import android.util.Log;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.LinkedBlockingDeque;

import sjtu.opennet.hon.Handlers;
import sjtu.opennet.hon.Textile;
import sjtu.opennet.stream.video.M3U8Util;
import sjtu.opennet.textilepb.Model;


public class VideoAddChunk_tkt extends Thread{
    private static final String TAG = "HONVIDEO.VideoStreamAddChunk";
    private String observeredDir;
    private String videoId;
    VideoAddChunk_tkt.ChunkListener chunkListener;
    VideoAddChunk_tkt.ChunkAdder chunkAdder;
    HashSet<String> chunkNames;
    LinkedBlockingDeque<ChunkPbInfo> chunkQueue;
    private boolean finishChunkAdd;


    public VideoAddChunk_tkt(String path, String videoId) {
        observeredDir=path;
        this.videoId=videoId;

        chunkNames=new HashSet<>();
        chunkQueue=new LinkedBlockingDeque<>();
        chunkListener=new VideoAddChunk_tkt.ChunkListener(observeredDir);
        chunkAdder=new VideoAddChunk_tkt.ChunkAdder();
    }

    @Override
    public void run() {
        finishChunkAdd = false;
        chunkListener.startWatching();
        chunkAdder.start();
    }

    public boolean finishAdd(){
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Model.VideoChunk virtualChunk = Model.VideoChunk.newBuilder()
                .setId(videoId)
                .setChunk("VIRTUAL")
                .setAddress("VIRTUAL")
                .setStartTime(-2)
                .setEndTime(-2)
                .setIndex(chunkNames.size()) //index from 0 to size-1, so the index of virtual chunk is size
                .build();
        try {
            Textile.instance().videos.addVideoChunk(virtualChunk);
            Textile.instance().videos.publishVideoChunk(virtualChunk);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d(TAG, "onComplete: add virtual chunk: " + chunkNames.size());

        finishChunkAdd=true;
        return finishChunkAdd;
    }

    class ChunkPbInfo{
        public String chunkName;
        public long chunkStartTime;
        public long chunkEndTime;
        public long chunkIndex;

        public ChunkPbInfo(String chunkName, long chunkStartTime, long chunkEndTime, long chunkIndex) {
            this.chunkName = chunkName;
            this.chunkStartTime = chunkStartTime;
            this.chunkEndTime = chunkEndTime;
            this.chunkIndex = chunkIndex;
        }
    }

    class ChunkListener extends FileObserver {

        long currentIndex = 0;
        long startTime = 0;

        public ChunkListener(String path) {
            super(path);
        }
        @Override
        public void onEvent(int event, @Nullable String path) {
            switch(event){
                case MOVED_TO:
                    ArrayList<M3U8Util.ChunkInfo> infos=M3U8Util.getInfos(observeredDir+"/"+path);
                    for(M3U8Util.ChunkInfo chk:infos){
                        if(!chunkNames.contains(chk.filename)){
                            chunkNames.add(chk.filename);

                            // make chunkPbInfo
                            ChunkPbInfo chunkPbInfo=new ChunkPbInfo(chk.filename,startTime,startTime+chk.duration,currentIndex);
                            startTime+=chk.duration;
                            currentIndex++;
                            chunkQueue.add(chunkPbInfo);
                        }
                    }
                    break;
            }
        }
    }

    class ChunkAdder extends Thread {
        @Override
        public void run() {
            while (!finishChunkAdd) {
                try {
                    ChunkPbInfo chunkPbInfo = chunkQueue.take(); // if queue is empty, thread will be blocked here
                    ipfsAddChunk(chunkPbInfo);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            // at this time, task finish
            chunkListener.stopWatching();
            Log.d(TAG, "run: chunkadder finished");
        }

        private long ipfsAddChunk(ChunkPbInfo chunkPbInfo) {
            String tsAbsolutePath = observeredDir + "/chunks/" + chunkPbInfo.chunkName;
            byte[] tsFileContent=new byte[]{};
            try {
                tsFileContent = Files.readAllBytes(Paths.get(tsAbsolutePath));
            } catch (Exception e) {
                e.printStackTrace();
            }
            Textile.instance().ipfs.ipfsAddData(tsFileContent, true, false, new Handlers.IpfsAddDataHandler() {
                @Override
                public void onComplete(String path) {
                    Model.VideoChunk videoChunk = Model.VideoChunk.newBuilder()
                            .setId(videoId)
                            .setChunk(chunkPbInfo.chunkName)
                            .setAddress(path)
                            .setStartTime(chunkPbInfo.chunkStartTime)
                            .setEndTime(chunkPbInfo.chunkEndTime)
                            .setIndex(chunkPbInfo.chunkIndex)
                            .build();
                    try {
                        Textile.instance().videos.addVideoChunk(videoChunk);
                        Textile.instance().videos.publishVideoChunk(videoChunk);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    Log.d(TAG, "onComplete: ipfsAddChunk: "+chunkPbInfo.chunkName+" "+chunkPbInfo.chunkIndex+" "+chunkPbInfo.chunkStartTime+" "+chunkPbInfo.chunkEndTime);
                }
                @Override
                public void onError(Exception e) { }
            });
            return 1;
        }

    }
}
