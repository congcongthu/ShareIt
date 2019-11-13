package sjtu.opennet.honvideo;

import android.os.FileObserver;
import android.util.Log;

import java.util.concurrent.BlockingQueue;

public class VideoFileListener extends FileObserver {
    private final String TAG = "HONVIDEO.VideoFileListener";
    private String observeredDir;
    private String videoId;     //required by VideoTask
    private BlockingQueue<VideoUploadTask> videoQueue;      //required bu VideoUploader
    private BlockingQueue<ChunkPublishTask> chunkQueue;     //VideoUploadTask will use this to add publish task
    public VideoFileListener(String path, String videoId, BlockingQueue<VideoUploadTask> videoQueue, BlockingQueue<ChunkPublishTask> chunkQueue){
        super(path);
        observeredDir = path;
        this.videoId = videoId;
        this.videoQueue = videoQueue;
        this.chunkQueue = chunkQueue;
    }

    @Override
    public void onEvent(int event, String path){
        switch(event){
            case FileObserver.CLOSE_WRITE:
                Log.d(TAG, String.format("%s closed at %d", path, System.currentTimeMillis()));
                //Log.d(TAG, String.format("Chunk Index :%d", getIndFromPath(path)));
                String tsAbsolutePath = String.format("%s/%s", observeredDir, path);
                VideoUploadTask currentTask = new VideoUploadTask(videoId, path, tsAbsolutePath, chunkQueue, false);
                try {
                    videoQueue.add(currentTask);
                }catch(Exception e){
                    e.printStackTrace();
                }

        }
    }
}
