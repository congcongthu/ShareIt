package sjtu.opennet.stream.video;

import android.os.FileObserver;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.protobuf.ByteString;
import com.googlecode.protobuf.format.JsonFormat;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.LinkedBlockingDeque;

import sjtu.opennet.hon.Textile;
import sjtu.opennet.textilepb.Model;
import sjtu.opennet.textilepb.View;

public class VideoStreamAddChunk extends Thread{
    private static final String TAG = "HONVIDEO.VideoStreamAddChunk";
    private String observeredDir;
    private String videoId;
    ChunkListener chunkListener;
    ChunkAdder chunkAdder;
    HashSet<String> chunkNames;
    LinkedBlockingDeque<M3U8Util.ChunkInfo> chunkQueue;
    private boolean finishChunkAdd;
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
        finishChunkAdd = false;
        chunkAdder.start();
        chunkListener.startWatching();
    }

    public boolean finishAdd(){
        try {
            Thread.sleep(10000);
            finishChunkAdd=true;
            Textile.instance().streams.closeStream(threadId,videoId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return finishChunkAdd;
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
            while(!finishChunkAdd){
                try {
                    M3U8Util.ChunkInfo chunk=chunkQueue.take();
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
                View.VideoDescription videoDescription= View.VideoDescription.newBuilder()
                        .setChunk(chunk.filename)
                        .setStartTime(currentDuration)
                        .setEndTime(currentDuration+chunk.duration)
                        .setIndex(currentIndex)
                        .build();
                String videoDescStr= JsonFormat.printToString(videoDescription);
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
