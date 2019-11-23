package sjtu.opennet.honvideo;

import android.os.FileObserver;
import android.util.Log;

import java.util.concurrent.BlockingQueue;

public class M3u8Listener extends FileObserver {
    private static final String TAG = "HONVIDEO.M3u8Listener";

    //private BlockingQueue<String> updateQueue;
    private String videoId;     //required by VideoTask
    private BlockingQueue<VideoUploadTask> videoQueue;      //required bu VideoUploader
    private BlockingQueue<ChunkPublishTask> chunkQueue;     //VideoUploadTask will use this to add publish task
    private String observeredDir;

    static class chunkInfo{
        public String filename;
        public long duration;
        chunkInfo(String filename, long duration){
            this.filename = filename;
            this.duration = duration;
        }
    }

    public  M3u8Listener(String path, String videoId, BlockingQueue<VideoUploadTask> videoQueue, BlockingQueue<ChunkPublishTask> chunkQueue){
        super(path);
        observeredDir = path;
        this.videoId = videoId;
        this.videoQueue = videoQueue;
        this.chunkQueue = chunkQueue;
        Log.d(TAG, path);
    }

    public static chunkInfo parseM3u8Content(String m3u8Content){
        String[] lines = m3u8Content.split("\n");
        String infoLine =lines[5];
        String filename = lines[6];

        //Log.d(TAG, String.format("Line: %s", infoLine));
        //Log.d(TAG, String.format("Line: %s", infoLine.substring(8, infoLine.length()-1)));
        long duration = (long)(Double.parseDouble(infoLine.substring(8, infoLine.length()-1))*1000000);
        return new chunkInfo(filename, duration);
    }

    @Override
    public void onEvent(int event, String path){
        String m3u8Content;
        switch(event){

            case FileObserver.MOVED_TO:
                Log.d(TAG, String.format("%s, MOVED_TO", path));

                m3u8Content = FileUtil.readAllString(String.format("%s/%s" ,observeredDir, path));
                Log.d(TAG, String.format("Whole list content:\n%s", m3u8Content));
                chunkInfo info = parseM3u8Content(m3u8Content);
                Log.d(TAG, String.format("File: %s, Duration: %d", info.filename, info.duration));
                String tsAbsolutePath = String.format("%s/chunks/%s", observeredDir, info.filename);
                VideoUploadTask currentTask = new VideoUploadTask(videoId, info.filename, tsAbsolutePath, info.duration, false);
                try {
                    videoQueue.add(currentTask);
                }catch(Exception e){
                    e.printStackTrace();
                }
                break;
        }
    }
}
