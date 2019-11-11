package com.sjtuopennetwork.shareit.util;

import android.os.FileObserver;
import android.util.Log;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Paths;

import sjtu.opennet.hon.Handlers;
import sjtu.opennet.hon.Textile;


public class VideoFileListener extends FileObserver {
    private final String TAG = "VideoFileListener";
    private String observeredDir;
    public VideoFileListener(String path){
        super(path);
        observeredDir = path;
    }
    private Handlers.IpfsAddDataHandler tsHandler = new Handlers.IpfsAddDataHandler() {
        @Override
        public void onComplete(String path) {
            Log.d(TAG, String.format("ts chunk ipfs path: %s", path));
            //Textile.instance().videos.addVideoChunk();
        }

        @Override
        public void onError(Exception e) {
            Log.e(TAG, String.format("ts upload failed"));
            e.printStackTrace();
        }
    };

    @Override
    public void onEvent(int event, String path){
        switch(event){
            case FileObserver.CLOSE_WRITE:
                Log.d(TAG, String.format("%s closed at %d", path, System.currentTimeMillis()));

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
