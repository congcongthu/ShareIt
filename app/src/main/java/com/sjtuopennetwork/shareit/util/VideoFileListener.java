package com.sjtuopennetwork.shareit.util;

import android.os.FileObserver;
import android.util.Log;

public class VideoFileListener extends FileObserver {
    private final String TAG = "VideoFileListener";
    private String observeredDir;
    public VideoFileListener(String path){
        super(path);
        observeredDir = path;
    }

    @Override
    public void onEvent(int event, String path){
        switch(event){
            case FileObserver.CLOSE_WRITE:
                Log.d(TAG, String.format("%s closed at %d", path, System.currentTimeMillis()));

                //addVideoChunk
        }
    }
}
