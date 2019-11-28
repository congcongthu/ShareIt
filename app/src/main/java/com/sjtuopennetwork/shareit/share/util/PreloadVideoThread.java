package com.sjtuopennetwork.shareit.share.util;

import android.content.Context;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.HashMap;
import java.util.Map;

import sjtu.opennet.hon.Textile;
import sjtu.opennet.honvideo.VideoReceiveHelper;
import sjtu.opennet.textilepb.Model;
import sjtu.opennet.textilepb.QueryOuterClass;

public class PreloadVideoThread extends Thread{
    private static final String TAG = "==============";

    boolean finished=false;
    boolean finishGetHash=false;

    String videoId;
    Map<String, String> addressMap;
    VideoReceiveHelper videoReceiveHelper;
    Model.Video video ;
    Context context;

    public PreloadVideoThread(Context context,String videoId){
        this.context=context;
        this.videoId=videoId;
        try {
            video = Textile.instance().videos.getVideo(videoId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        videoReceiveHelper= new VideoReceiveHelper(context, video);
        addressMap=new HashMap<>();

        if(!EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().register(this);
        }
    }


    public void searchTheChunk(String videoid,String chunkName){
        QueryOuterClass.QueryOptions options = QueryOuterClass.QueryOptions.newBuilder()
                .setWait(1)
                .setLimit(1)
                .build();
        QueryOuterClass.VideoChunkQuery query=QueryOuterClass.VideoChunkQuery.newBuilder()
                .setStartTime(-1)
                .setEndTime(-1)
                .setChunk(chunkName)
                .setId(videoid).build();
        try {
            Textile.instance().videos.searchVideoChunks(query,options);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void getAnResult(Model.VideoChunk videoChunk){
        if(videoChunk.getId().equals(videoId)){ //保存到自己的里面
            addressMap.put(videoChunk.getChunk(),videoChunk.getAddress()); //拿到一个结果就放进来一个，可能会相同
            videoReceiveHelper.receiveChunk(videoChunk); //将对应的视频保存到本地
        }
    }

    @Override
    public void run() {
        int i=0;
        long videoLength=video.getVideoLength();
        Model.VideoChunk v=null;
        while(!finished){
            String chunkName="out"+String.format("%04d", i)+".ts";
            try{
                v= Textile.instance().videos.getVideoChunk(videoId, chunkName);
                if (v == null) {
                    while(!finishGetHash){ //本地没有就一直去找，直到找到为止
                        v = Textile.instance().videos.getVideoChunk(videoId, chunkName);
                        if (v != null) //如果已经获取到了ts文件
                            break;
                        System.out.println("==========获取chunk："+chunkName);
                        if(!addressMap.containsKey(chunkName)) { //如果还没有ts的hash就去找hash
                            System.out.println("======gettinghash："+chunkName);
                            Log.d(TAG, "run: gettinghash："+finished);
                            searchTheChunk(videoId,chunkName);
                        }else{
                            System.out.println("======获取到hash了："+chunkName);
                        }
                        try {
                            sleep(1500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                if(v.getEndTime()>=videoLength - 200000){ //如果是最后一个就终止,videolength是微秒，
                    finished=true;
                }
                Log.d(TAG, "run: 获得视频的序号："+i+ " 当前状态(finished finishGetHash)："+finished+" "+finishGetHash);
                i++;
                if(i>2){ //预加载3块，012
                    break;
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public void stopPreload(){
        finished=true;
        finishGetHash=true;
    }
}
