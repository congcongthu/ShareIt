package sjtu.opennet.honvideo;

import android.util.Log;

import java.util.Set;

import sjtu.opennet.hon.Handlers;
import sjtu.opennet.hon.Textile;
import sjtu.opennet.textilepb.Model;

public class VideoReceiveTask implements Comparable<VideoReceiveTask>{
    private final String TAG = "HONVIDEO.VideoReceiveTask";
    //private String videoId;
    private boolean endTag;
    private Model.VideoChunk vChunk;
    private String chunkDir;
    private String fileName;
    private int chunkIndex;



    public int chunkStartTime;

    private Handlers.DataHandler handler = new Handlers.DataHandler() {
        @Override
        public void onComplete(byte[] data, String media) {
            String savePath = String.format("%s/%s", chunkDir, fileName);
            FileUtil.writeByteArrayToFile(savePath, data);
            Log.d(TAG, String.format("Save chunk to %s", savePath));
        }

        @Override
        public void onError(Exception e) {
            Log.e(TAG, "ipfs.dataAtPath throws unexpected error when receiving video chunks.");
            e.printStackTrace();
        }
    };

    VideoReceiveTask(Model.VideoChunk vChunk, String chunkDir, boolean endTag){
        //this.videoId = videoId;
        this.vChunk = vChunk;
        this.chunkDir = chunkDir;
        this.endTag = endTag;
        if(!endTag) {
            chunkStartTime = vChunk.getStartTime();
            fileName = vChunk.getChunk();
            chunkIndex = Segmenter.getIndFromPath(fileName);
        }
    }

    public String getFileName(){
        return fileName;
    }

    public static VideoReceiveTask endTask(){
        return new VideoReceiveTask(null, "", true);
    }

    public boolean isEnd(){
        return endTag;
    }

    @Override
    public int compareTo(VideoReceiveTask another){
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
     * process each video receive task. It does following things:<br />
     *  - Get chunk content from hash.
     *  - Build the ts file and save to local storage.
     *  - @TODO Write the m3u8 list file.
     */
    public boolean process(){
        if(endTag){
            return true;
        }
        try {
            String chunkHash = vChunk.getAddress();
            Textile.instance().ipfs.dataAtPath(chunkHash, handler);
            Log.d(TAG, String.format("Task with file %s completes.", fileName));
            return true;
        }catch(Exception e){
            Log.e(TAG, String.format("Task with file %s fail!!!!! Add it back to queue later.", fileName));
            e.printStackTrace();
            return false;
        }
        //return fileName;
    }
}
