package com.sjtuopennetwork.shareit.util;

import android.util.Log;

import java.util.concurrent.BlockingQueue;

/**
 * Upload video in a async way.
 */

public class VideoUploader extends Thread{
    private BlockingQueue<VideoTask> videoQueue;
    private final String TAG = "VideoUploader";

    public VideoUploader(BlockingQueue<VideoTask> bQueue){
        videoQueue = bQueue;
    }

    public void shutDown(){
        Log.w(TAG, "Video Uploader Shut Down!!");
        interrupt();
    }

    @Override
    public void run(){
        VideoTask vTask;
        try{
            vTask = videoQueue.take();
        }catch(InterruptedException ie){
            Log.e(TAG, "Unexpected Interrupt.");
            ie.printStackTrace();
            interrupt();
            
        }
    }
}
