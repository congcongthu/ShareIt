package sjtu.opennet.stream.video;

import android.os.FileObserver;
import android.support.annotation.Nullable;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.ByteString;


import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.LinkedBlockingDeque;

import sjtu.opennet.hon.Textile;
import sjtu.opennet.stream.util.FileUtil;
import sjtu.opennet.textilepb.Model;

public class VideoStreamAddChunk extends Thread{
    private static final String TAG = "=======================HONVIDEO.VideoStreamAddChunk";
    private String observeredDir;
    private String videoId;
    ChunkListener chunkListener;
    ChunkAdder chunkAdder;
    HashSet<String> chunkNames;
    LinkedBlockingDeque<M3U8Util.ChunkInfo> chunkQueue;
//    private boolean finishChunkAdd;
    private String threadId;


    public VideoStreamAddChunk(String path, String videoId, String threadId) {
        observeredDir=path;
        this.videoId=videoId;
        this.threadId=threadId;

        chunkNames=new HashSet<>();
        chunkQueue=new LinkedBlockingDeque<>();
        chunkListener=new ChunkListener(observeredDir);
        chunkAdder=new ChunkAdder();
    }

    @Override
    public void run() {
        chunkAdder.start();
        chunkListener.startWatching();
    }

    public void finishAdd(){
        try {
            Textile.instance().streams.closeStream(threadId,videoId);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // delete file
        FileUtil.deleteDirectory(new File(observeredDir));
    }

    public void finishSegment(){
        chunkQueue.add(new M3U8Util.ChunkInfo("VIRTUAL",0));
    }

    class ChunkListener extends FileObserver{
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
                            chunkQueue.add(chk);
                        }
                    }
                    break;
            }
        }
    }

    class ChunkAdder extends Thread{
        @Override
        public void run() {
            int currentIndex=0;
            long currentDuration=0;
            while(true){
                try {
                    M3U8Util.ChunkInfo chunk=chunkQueue.take();
                    if(chunk.duration==0){
                        finishAdd();
                        break;
                    }
                    currentDuration+=streamAddFile(chunk,currentIndex,currentDuration);
                    currentIndex++;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            // at this time, task finish
            chunkListener.stopWatching();
            Log.d(TAG, "run: chunkadder finished");
        }

        private long streamAddFile(M3U8Util.ChunkInfo chunk,int currentIndex,long currentDuration){
            String tsAbsolutePath=observeredDir+"/chunks/"+chunk.filename;
            try {
                byte[] tsFileContent = Files.readAllBytes(Paths.get(tsAbsolutePath));
                JSONObject object=new JSONObject();
                object.put("startTime",String.valueOf(currentDuration));
                object.put("endTime",String.valueOf(currentDuration+chunk.duration));
                String videoDescStr= JSON.toJSONString(object);
                Log.d(TAG, "streamAddFile: "+videoDescStr);
                Model.StreamFile streamFile= Model.StreamFile.newBuilder()
                        .setData(ByteString.copyFrom(tsFileContent))

                        .setDescription(ByteString.copyFromUtf8(videoDescStr))
                        .build();
                Textile.instance().streams.streamAddFile(videoId,streamFile.toByteArray());
            } catch (Exception e) {
                e.printStackTrace();
            }
            return chunk.duration;
        }
    }
}
