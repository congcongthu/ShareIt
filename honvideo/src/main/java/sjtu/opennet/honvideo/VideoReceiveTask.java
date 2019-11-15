package sjtu.opennet.honvideo;

import android.util.Log;

import java.util.Set;
import java.util.concurrent.BlockingQueue;

import sjtu.opennet.hon.Handlers;
import sjtu.opennet.hon.Textile;
import sjtu.opennet.textilepb.Model;

public class VideoReceiveTask implements Comparable<VideoReceiveTask>{
    private final String TAG = "HONVIDEO.VideoReceiveTask";
    //private String videoId;
    private boolean endTag;             //endTag has lowest priority. It is used to end thread when queue is empty.
    private Model.VideoChunk vChunk;
    private String chunkDir;
    private String fileName;
    //private int chunkIndex;
    private boolean destroyTag;         //destroyTag has highest priority. It is used to end thread when activity is destroy.



    private BlockingQueue<Integer> lockQueue;
    public int chunkStartTime;

    private Handlers.DataHandler handler = new Handlers.DataHandler() {
        @Override
        public void onComplete(byte[] data, String media) {
            Log.d(TAG, String.format("Task %s IPFS dataAtPath complete", fileName));
            String savePath = String.format("%s/%s", chunkDir, fileName);
            FileUtil.writeByteArrayToFile(savePath, data);
            Log.d(TAG, String.format("Save chunk to %s", savePath));
            try {
                Textile.instance().videos.addVideoChunk(vChunk);
                lockQueue.add(1);
            }catch(Exception e){
                Log.e(TAG, "Write Database Fail!!!!");
                lockQueue.add(-1);
                e.printStackTrace();
            }
        }

        @Override
        public void onError(Exception e) {
            lockQueue.add(-1);
            Log.e(TAG, "ipfs.dataAtPath throws unexpected error when receiving video chunks.");
            e.printStackTrace();
        }
    };

    VideoReceiveTask(Model.VideoChunk vChunk, String chunkDir, boolean endTag, boolean destroyTag){
        //this.videoId = videoId;
        this.vChunk = vChunk;
        this.chunkDir = chunkDir;
        this.endTag = endTag;
        this.destroyTag = destroyTag;
        if(!endTag && !destroyTag) {
            chunkStartTime = vChunk.getStartTime();
            fileName = vChunk.getChunk();
            //chunkIndex = Segmenter.getIndFromPath(fileName);
        }
    }

    public String getFileName(){
        return fileName;
    }

    public static VideoReceiveTask endTask(){
        return new VideoReceiveTask(null, "", true, false);
    }
    public static VideoReceiveTask destroyTask(){
        return new VideoReceiveTask(null, "", false, true);
    }
    public boolean isEnd(){
        return endTag;
    }
    public boolean isDestroy(){
        return destroyTag;
    }
    @Override
    public int compareTo(VideoReceiveTask another){
        // end task has the lowest priority.
        if(this.destroyTag){
            return -1;
        }
        if(another.destroyTag){
            return 1;
        }
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
    public boolean process(BlockingQueue<Integer> lockQueue){
        this.lockQueue = lockQueue;
        if(endTag||destroyTag){
            Log.e(TAG, "This should not happen!!!!!. Return true for end or destroy task.");
            lockQueue.add(0);
            return true;
        }
        try {
            String chunkHash = vChunk.getAddress();
            Log.d(TAG, String.format("IPFS get %s from %s", fileName, chunkHash));
            Textile.instance().ipfs.dataAtPath(chunkHash, handler);
            //Log.d(TAG, String.format("Task with file %s completes.", fileName));
            return true;
        }catch(Exception e){
            Log.e(TAG, String.format("Task with file %s fail!!!!! Add it back to queue later.", fileName));
            e.printStackTrace();
            return false;
        }
        //return fileName;
    }
}
