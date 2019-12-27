package sjtu.opennet.honvideo;

import android.os.FileObserver;
import android.util.Log;

import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;

public class M3u8Listener2 extends FileObserver {
    private static final String TAG = "HONVIDEO.M3u8Listener2";

    //private BlockingQueue<String> updateQueue;
    private String videoId;     //required by VideoTask
    private BlockingQueue<VideoUploadTask> videoQueue;      //required bu VideoUploader
    private String observeredDir;
    //private long currentIndex = 0;
    private String currentFilename;



    public  M3u8Listener2(String path, String videoId, BlockingQueue<VideoUploadTask> videoQueue){
        super(path);
        observeredDir = path;
        this.videoId = videoId;
        this.videoQueue = videoQueue;
        Log.d(TAG, path);
    }

    private void updateCurrent(ArrayList<m3u8Helper.chunkInfo> infos){
        boolean update = false;
        if(currentFilename == null){
            update = true;
        }
        for (m3u8Helper.chunkInfo info: infos){
            if (update){
                String tsAbsolutePath = String.format("%s/chunks/%s", observeredDir, info.filename);
                VideoUploadTask currentTask = new VideoUploadTask(videoId, info.filename, tsAbsolutePath, info.duration, false);
                try {
                    Log.d(TAG, String.format("Listener: file %s", info.filename));
                    videoQueue.add(currentTask);
                }catch(Exception e){
                    e.printStackTrace();
                }
                currentFilename = info.filename;
            } else {
                if (currentFilename.equals(info.filename)){
                    update = true;
                }
            }
        }
//        if(!update){
//
//        }
    }


    @Override
    public void onEvent(int event, String path){
        String m3u8Content;
        switch(event){

            case FileObserver.MOVED_TO:
                //Log.d(TAG, String.format("%s, MOVED_TO", path));

                //m3u8Content = FileUtil.readAllString(String.format("%s/%s" ,observeredDir, path));
                //Log.d(TAG, String.format("Whole list content:\n%s", m3u8Content));
                ArrayList<m3u8Helper.chunkInfo> infos = m3u8Helper.getInfos(String.format("%s/%s" ,observeredDir, path));
                updateCurrent(infos);
//                chunkInfo info = parseM3u8Content(m3u8Content);
//                Log.d(TAG, String.format("Listener: File: %s, Duration: %d", info.filename, info.duration));
//                String tsAbsolutePath = String.format("%s/chunks/%s", observeredDir, info.filename);
//                VideoUploadTask currentTask = new VideoUploadTask(videoId, info.filename, tsAbsolutePath, info.duration, false);
//                try {
//                    videoQueue.add(currentTask);
//                }catch(Exception e){
//                    e.printStackTrace();
//                }
                break;
        }
    }
}
