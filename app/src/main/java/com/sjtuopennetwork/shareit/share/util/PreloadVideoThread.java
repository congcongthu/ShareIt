package com.sjtuopennetwork.shareit.share.util;

import android.content.Context;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import sjtu.opennet.hon.Textile;
import sjtu.opennet.honvideo.VideoHandlers;
import sjtu.opennet.honvideo.VideoReceiveHelper;
import sjtu.opennet.honvideo.VideoUploadHelper;
import sjtu.opennet.textilepb.Model;
import sjtu.opennet.textilepb.QueryOuterClass;

public class PreloadVideoThread extends Thread{
    private static final String TAG = "PreloadVideoThread";

//    boolean finished=false;
//    boolean finishGetHash=false;

    String videoId;
    VideoReceiveHelper videoReceiveHelper;
    Model.Video video ;
    Context context;
    String dir;


    private int writeM3u8Count;

//    final Object WAITLOCK = new Object();

    public PreloadVideoThread(Context context,String videoId){
        this.context=context;
        this.videoId=videoId;
        try {
            video = Textile.instance().videos.getVideo(videoId);
        } catch (Exception e) {
            e.printStackTrace();
        }

        dir = VideoUploadHelper.getVideoPathFromID(context, videoId);

        writeM3u8Count=0;

        videoReceiveHelper=new VideoReceiveHelper(context, video, new VideoHandlers.ReceiveHandler() {
            @Override
            public void onChunkComplete(Model.VideoChunk vChunk) {

            }

            @Override
            public void onVideoComplete() {

            }

            @Override
            public void onError(Exception e) {

            }
        });



//        videoReceiveHelper= new VideoReceiveHelper(context, video);
//        addressMap=new HashMap<>();

//        if(!EventBus.getDefault().isRegistered(this)){
//            EventBus.getDefault().register(this);
//        }
    }

//
//    public void searchTheChunk(String videoid, String chunkName, long waitTime){
//        QueryOuterClass.QueryOptions options = QueryOuterClass.QueryOptions.newBuilder()
//                .setWait(1)
//                .setLimit(1)
//                .build();
//        QueryOuterClass.VideoChunkQuery query=QueryOuterClass.VideoChunkQuery.newBuilder()
//                .setStartTime(-1)
//                .setEndTime(-1)
//                .setChunk(chunkName)
//                .setId(videoid).build();
//        try {
//            synchronized (WAITLOCK) {
//                Textile.instance().videos.searchVideoChunks(query, options);
//                WAITLOCK.wait(waitTime);    //Wait for waitTime ms at most. It can be notified by getAnResult.
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    @Subscribe(threadMode = ThreadMode.BACKGROUND)
//    public void getAnResult(Model.VideoChunk videoChunk){
//        synchronized (WAITLOCK) {
//            if (videoChunk.getId().equals(videoId)) { //保存到自己的里面
//                addressMap.put(videoChunk.getChunk(), videoChunk.getAddress()); //拿到一个结果就放进来一个，可能会相同
//                WAITLOCK.notify();
//                videoReceiveHelper.receiveChunk(videoChunk); //将对应的视频保存到本地
//            }
//        }
//    }

    @Override
    public void run() {

        videoReceiveHelper.preloadVideo(2);
//        int i=0;
//        long videoLength=video.getVideoLength();
//        Model.VideoChunk v=null;
//        while(!finished){
//            String chunkName="out"+String.format("%04d", i)+".ts";
//            try{
//                v= Textile.instance().videos.getVideoChunk(videoId, chunkName);
//                if (v == null) {
//                    while (true) { //本地没有就一直去找，直到找到为止
//                        synchronized (WAITLOCK){
//                            Log.d(TAG, String.format("VIDEOPIPELINE: %s try to get", chunkName));
//                            v = Textile.instance().videos.getVideoChunk(videoId, chunkName);
//                            if (v != null) //如果已经获取到了ts文件
//                                break;
//                            if (!addressMap.containsKey(chunkName)) { //如果还没有ts的hash就去找hash
//                                searchTheChunk(videoId, chunkName, 1000);
//                            }
//                        }
//                    }
//                }
//                if(v.getEndTime()>=videoLength - 200000){ //如果是最后一个就终止,videolength是微秒，
//                    finished=true;
//                }
//                Log.d(TAG, "run: 获得视频的序号："+i+ " 当前状态(finished finishGetHash)："+finished+" "+finishGetHash);
//                i++;
//                if(i>2){ //预加载3块，012
//                    break;
//                }
//            }catch (Exception e){
//                e.printStackTrace();
//            }
//        }
    }

//    public void stopPreload(){
//        finished=true;
//        finishGetHash=true;
//    }
}
