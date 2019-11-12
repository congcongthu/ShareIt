package com.sjtuopennetwork.shareit.util;

import android.util.Log;

import org.apache.commons.io.input.TailerListenerAdapter;

public class VideoListListener extends TailerListenerAdapter{
    private final String TAG = "VideoListListener";
    //private String observeredList;
    private int counter;
    VideoListListener(){
        super();
        //observeredList = listPath;
        counter = 0;
    }

    @Override
    public void handle(final String line){
        Log.d(TAG, String.format("%d:\t%s", counter, line));
        counter = counter + 1;
    }

}
