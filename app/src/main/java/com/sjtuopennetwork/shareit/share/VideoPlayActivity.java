package com.sjtuopennetwork.shareit.share;

import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.MediaController;
import android.widget.VideoView;

import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.hls.playlist.HlsMediaPlaylist;
import com.google.android.exoplayer2.source.hls.playlist.HlsPlaylist;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.sjtuopennetwork.shareit.R;
import com.sjtuopennetwork.shareit.util.FileUtil;
//import com.sjtuopennetwork.shareit.util.VideoHelper;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import sjtu.opennet.hon.Handlers;
import sjtu.opennet.hon.Textile;
import sjtu.opennet.honvideo.Segmenter;
import sjtu.opennet.honvideo.VideoReceiveHelper;
import sjtu.opennet.honvideo.VideoReceiver;
import sjtu.opennet.honvideo.VideoUploadHelper;
import sjtu.opennet.textilepb.Model;
import sjtu.opennet.textilepb.QueryOuterClass;


public class VideoPlayActivity extends AppCompatActivity {

    //内存数据
    String videoid;
    HlsMediaSource hlsMediaSource;
    DataSource.Factory dataSourceFactory;
    SimpleExoPlayer player;
    VideoReceiveHelper videorHelper;
    ConcatenatingMediaSource mediaSource;
    int videoLenth;
    File m3u8file;
    String dir;
    int m3u8WriteCount;
    Model.Video video;
    boolean finished;
    static int gap = 100;

    //两个线程
    GetChunkThread getChunkThread=new GetChunkThread();
    PlayVideoThread playVideoThread=new PlayVideoThread();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_play);

        m3u8WriteCount=0;
        if(!EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().register(this);
        }

        videoid = getIntent().getStringExtra("videoid");
        System.out.println("=================videoID："+videoid);
        try {
            video = Textile.instance().videos.getVideo(videoid);
            videoLenth = video.getVideoLength();
            System.out.println("=============视频长度："+videoLenth);
            videorHelper = new VideoReceiveHelper(this, video);
        } catch (Exception e) {
            e.printStackTrace();
        }

        dir = VideoUploadHelper.getVideoPathFromID(this, videoid);
        initM3u8();
        System.out.println("=======================初始化dir:"+dir);

        if(DownloadComplete(videoid)){
            System.out.println("===================完全下载了");
            finished = true;
            writeCompleteM3u8();
        }else{ //没有完全下载下来，去网络中查找
            searchVideoChunks();
            getChunkThread.start();
            while (true){
                if((finished && m3u8WriteCount>0) || (m3u8WriteCount > 1 ) ) //如果还没有写m3u8，主线程就等待，直到写了2次之后就开始播放
                    break;
            }
        }
        //一个线程在按顺序遍历，没有就循环去取，先取本地再取网络，取到为止,取到就写m3u8文件。
        System.out.println("开始播放");
        PlayerView playerView = findViewById(R.id.player_view);
        player = ExoPlayerFactory.newSimpleInstance(VideoPlayActivity.this);
        playerView.setPlayer(player);
        dataSourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(VideoPlayActivity.this, "ShareIt"));
        hlsMediaSource = new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.fromFile(m3u8file));
        player.prepare(hlsMediaSource);
    }

    public static boolean DownloadComplete(String vid){
        List<Model.VideoChunk> tmp;
        try {
            tmp = Textile.instance().videos.chunksByVideoId(vid).getItemsList();
        } catch (Exception e) {
            tmp = new ArrayList<>();
        }
        if(tmp.size() == 0) {
            System.out.println("no local chunks");
            return false;
        }
        int maxEndtime = 0;
        String lastChunk = "";
        for (Model.VideoChunk c : tmp){
            if (c.getEndTime() > maxEndtime) {
                maxEndtime = c.getEndTime();
                lastChunk = c.getChunk();
            }
        }
        System.out.println("max end time: "+ maxEndtime);

        try {
            Model.Video tmpVideo = Textile.instance().videos.getVideo(vid);
            if (tmpVideo == null) {
                System.out.println("no local video");
                return false;
            }
            int id = Segmenter.getIndFromPath(lastChunk);
            System.out.println("video length:"+tmpVideo.getVideoLength());
            System.out.println(tmp.size());
            if (maxEndtime >= tmpVideo.getVideoLength()- gap && id == tmp.size()-1){
                System.out.println("download complete!=================");
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public class GetChunkThread extends Thread{
        @Override
        public void run() {
            int i=0;
            boolean alreadyHave;
            while(!finished){
                alreadyHave=false;
                String chunkName="out"+String.format("%04d", i)+".ts";
                System.out.println("===================chunkName+"+chunkName);
                try {
                    Model.VideoChunk v=Textile.instance().videos.getVideoChunk(videoid, chunkName);
                    if ( v!=null ){ //本地有了，就写m3u8文件
                        writeM3u8(v);
                        if(v.getEndTime()>=videoLenth - gap){ //如果是最后一个就终止
                            finished=true;
                        }
                    }
                    else {
                        while(Textile.instance().videos.getVideoChunk(videoid, chunkName) == null){ //本地没有就一直去找，直到找到为止
                            System.out.println("Getting "+chunkName);
                            searchTheChunk(chunkName);
                            try {
                                sleep(2000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    i++; //处理下一个视频
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void writeCompleteM3u8(){
        String m3u8=dir+"/chunks/playlist.m3u8";
        File m3u8file=new File(m3u8);
        int i=0;
        while(true){
            String chunkName="out"+String.format("%04d", i)+".ts";
            try {
                Model.VideoChunk v=Textile.instance().videos.getVideoChunk(videoid, chunkName);
                if ( v!=null ){ //本地有了，就写m3u8文件
                    writeM3u8(v);
                }
                else {
                    break;
                }
                i++; //处理下一个视频
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        writeM3u8End();
    }

    public void initM3u8(){
        String m3u8=dir+"/chunks/playlist.m3u8";
        File m3u8file=new File(m3u8);
        try{
            if(!m3u8file.exists()){ //从dir中找到文件复制到chunks里面
                m3u8file.createNewFile();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        String head="#EXTM3U\n" +
                    "#EXT-X-VERSION:3\n" +
                    "#EXT-X-MEDIA-SEQUENCE:0\n" +
                    "#EXT-X-ALLOW-CACHE:YES\n" +
                    "#EXT-X-TARGETDURATION:15\n" +
                    "#EXT-X-PLAYLIST-TYPE:EVENT\n";
        try {
            FileWriter fileWriter = new FileWriter(m3u8);
            fileWriter.write(head);
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("===========m3u8文件初始化");
    }

    public void writeM3u8(Model.VideoChunk v){
        m3u8file=new File(dir+"/chunks/playlist.m3u8");
        int duration0=v.getEndTime()-v.getStartTime(); //毫秒
        float size = (float)duration0/1000;
        DecimalFormat df = new DecimalFormat("0.000000");//格式化小数，不足的补0
        String duration = df.format(size);//返回的是String类型的
        try {
            FileWriter fileWriter = new FileWriter(m3u8file,true);
            String append = "#EXTINF:"+duration+",\n"+
                            v.getChunk()+"\n";
            fileWriter.write(append);
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        m3u8WriteCount++;
        System.out.println("=================m3u8写的次数："+m3u8WriteCount);
    }

    public void writeM3u8End(){
        m3u8file=new File(dir+"/chunks/playlist.m3u8");
        try {
            FileWriter fileWriter = new FileWriter(m3u8file,true);
            String append = "#EXT-X-ENDLIST";
            fileWriter.write(append);
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public class PlayVideoThread extends Thread{
        @Override
        public void run() {

        }
    }

    public void searchTheChunk(String chunkName){
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

    public  void searchVideoChunks(){
        QueryOuterClass.QueryOptions options = QueryOuterClass.QueryOptions.newBuilder()
                .setWait(1)
                .setLimit(1000)
                .build();
        QueryOuterClass.VideoChunkQuery query=QueryOuterClass.VideoChunkQuery.newBuilder()
                .setStartTime(-1)
                .setEndTime(-1)
                .setId(videoid).build();
        try {
            Textile.instance().videos.searchVideoChunks(query,options);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void getAnResult(Model.VideoChunk videoChunk){
        System.out.println("==============得到videoChunk:"+videoChunk.getId()
            +" "+videoChunk.getAddress()+" "+videoChunk.getEndTime());
        videorHelper.receiveChunk(videoChunk); //保存到本地
    }

    @Override
    public void onStop() {
        super.onStop();
        if(EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().unregister(this);
        }
    }
}
