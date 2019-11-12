package com.sjtuopennetwork.shareit.util;

import android.os.FileObserver;
import android.util.Log;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.BlockingQueue;

import sjtu.opennet.hon.Handlers;
import sjtu.opennet.hon.Textile;


public class VideoFileListener extends FileObserver {
    private final String TAG = "VideoFileListener";
    private String observeredDir;
    private String videoId;     //required by VideoTask
    private BlockingQueue<VideoTask> videoQueue;    //required bu VideoUploader
    public VideoFileListener(String path, String videoId, BlockingQueue<VideoTask> videoQueue){
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
                VideoTask currentTask = new VideoTask(videoId, path, tsAbsolutePath, false);
                try {
                    videoQueue.add(currentTask);
                }catch(Exception e){
                    e.printStackTrace();
                }
                //addVideoChunk
                //String fileAbsolutePath = String.format("%s/%s", observeredDir, path);
                /*
                try {
                    byte[] fileContent = Files.readAllBytes(Paths.get(observeredDir, path));
                    Textile.instance().ipfs.ipfsAddData(fileContent, true, false, tsHandler);
                }catch(IOException ie){
                    ie.printStackTrace();
                }

                 */
        }
    }
}
