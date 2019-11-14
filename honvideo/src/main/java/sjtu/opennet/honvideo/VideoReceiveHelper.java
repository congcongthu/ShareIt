package sjtu.opennet.honvideo;

import android.content.Context;
import android.util.Log;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

import sjtu.opennet.hon.Textile;


import sjtu.opennet.hon.Handlers;

import sjtu.opennet.textilepb.Model;
import sjtu.opennet.textilepb.QueryOuterClass;

/**
 * VideoReceiveHelper does following things:<br />
 *  - Get video meta from video proto.<br />
 *  - Get video chunks<br />
 *  - Create video receive task based on video chunk.<br />
 *  - Add receive task to priority queue.<br />
 *  - Call the Receiver to handle the tasks in the queue.
 */
public class VideoReceiveHelper {
    private final String TAG = "HONVIDEO.VideoReceiveHelper";
    //private String videoId;

    //Variables from parameters
    private Model.Video videoPb;
    private Context context;
    //Variables got from video pb
    private String videoId;
    private int totalDuration;

    //Variables set during running
    private String videoPath;
    private String chunkPath;
    private VideoReceiver receiver;

    private BlockingQueue<VideoReceiveTask> vQueue;

    public VideoReceiveHelper(Context context, Model.Video videoPb){
        this.context = context;
        this.videoPb = videoPb;
        videoId = videoPb.getId();
        totalDuration = videoPb.getVideoLength();
        vQueue = new PriorityBlockingQueue<>();
        buildWorkspace();
        receiver = new VideoReceiver(vQueue, videoId, videoPath, chunkPath);
        receiver.start();
    }

    public void receiveChunk(Model.VideoChunk videoChunk){
        if(vQueue.contains(videoChunk)){
            Log.d(TAG, String.format("Chunk with %s already in task queue.", videoChunk.getChunk()));
            return;
        }
        vQueue.add(new VideoReceiveTask(videoChunk, chunkPath, false, false));
    }

    /**
     * shutDownReceiver is used to stop the receiver binding with this helper.
     * It will do following things:<br />
     *  - Run a new thread so the main thread would not be blocked.<br />
     *  - Sleep for 1 second to make sure that there is no chunk left if it is called after receiving all chunks.<br />
     *  - Add the end task to videoQueue to notify the VideoReceiver that these are all the task.
     */
    public class shutDownReceiver extends Thread{
        @Override
        public void run(){
            try {
                Thread.sleep(1000);
                vQueue.add(VideoReceiveTask.endTask());
            }catch(InterruptedException ie){
                ie.printStackTrace();
            }
        }
    }
    private void buildWorkspace(){
        String rootPath = FileUtil.getAppExternalPath(context, "video");
        videoPath = FileUtil.getAppExternalPath(context, String.format("video/%s", videoId));
        chunkPath = FileUtil.getAppExternalPath(context, String.format("video/%s/chunks", videoId));
    }

}
