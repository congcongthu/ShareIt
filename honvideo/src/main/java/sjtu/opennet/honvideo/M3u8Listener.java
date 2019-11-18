package sjtu.opennet.honvideo;

import android.os.FileObserver;
import android.util.Log;

import java.util.concurrent.BlockingQueue;

public class M3u8Listener extends FileObserver {
    private static final String TAG = "HONVIDEO.FileObserver";

    private BlockingQueue<String> updateQueue;

    public  M3u8Listener(String path){
        super(path);

    }

    @Override
    public void onEvent(int event, String path){
        switch(event){
            case FileObserver.CLOSE_WRITE:
//                Log.d(TAG, String.format("%s closed at %d", path, System.currentTimeMillis()));
//                Log.d(TAG, String.format("Chunk Index :%d", getIndFromPath(path)));
//                String tsAbsolutePath = String.format("%s/%s", observeredDir, path);
//                VideoUploadTask currentTask = new VideoUploadTask(videoId, path, tsAbsolutePath, chunkQueue, false);
//                try {
//                    videoQueue.add(currentTask);
//                }catch(Exception e){
//                    e.printStackTrace();
//                }
                String m3u8Content = FileUtil.readAllString(path);
                Log.d(TAG, m3u8Content);
        }
    }


}