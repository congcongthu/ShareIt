package sjtu.opennet.honvideo;

import android.os.FileObserver;
import android.util.Log;

import java.util.concurrent.BlockingQueue;

public class VideoFileListener extends FileObserver {
    private final String TAG = "HONVIDEO.VideoFileListener";
    private String observeredDir;
    private String videoId;     //required by VideoTask
    private BlockingQueue<VideoUploadTask> videoQueue;    //required bu VideoUploader
    public VideoFileListener(String path, String videoId, BlockingQueue<VideoUploadTask> videoQueue){
        super(path);
        observeredDir = path;
        this.videoId = videoId;
        this.videoQueue = videoQueue;
    }


    @Override
    public void onEvent(int event, String path){
        switch(event){
            case FileObserver.CLOSE_WRITE:
                Log.d(TAG, String.format("%s closed at %d", path, System.currentTimeMillis()));
                String tsAbsolutePath = String.format("%s/%s", observeredDir, path);
                VideoUploadTask currentTask = new VideoUploadTask(videoId, path, tsAbsolutePath, false);
                try {
                    videoQueue.add(currentTask);
                }catch(Exception e){
                    e.printStackTrace();
                }

        }
    }
}
