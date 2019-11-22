package com.sjtuopennetwork.shareit.share;

import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.VideoView;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.hls.playlist.HlsMediaPlaylist;
import com.google.android.exoplayer2.source.hls.playlist.HlsPlaylist;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

//import fi.iki.elonen.NanoHTTPD;
import sjtu.opennet.hon.Handlers;
import sjtu.opennet.hon.Textile;
import sjtu.opennet.honvideo.Segmenter;
import sjtu.opennet.honvideo.VideoReceiveHelper;
import sjtu.opennet.honvideo.VideoReceiver;
import sjtu.opennet.honvideo.VideoUploadHelper;
import sjtu.opennet.textilepb.Model;
import sjtu.opennet.textilepb.QueryOuterClass;


public class VideoPlayActivity extends AppCompatActivity {

    private static final String TAG = "=================";

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
    static int gap = 100000;
    Map<String, String> addressMap;
    private ProgressBar mProgressBar;
    FileWriter fileWriter;

    //两个线程
    GetChunkThread getChunkThread=new GetChunkThread();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_play);

        m3u8WriteCount=0;
        addressMap = new HashMap<>();
        if(!EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().register(this);
        }

        videoid = getIntent().getStringExtra("videoid");

        try {
            video = Textile.instance().videos.getVideo(videoid);
            videorHelper = new VideoReceiveHelper(this, video);
            dir = VideoUploadHelper.getVideoPathFromID(this, videoid);
            videoLenth = video.getVideoLength();
            Log.d(TAG, "onCreate: 视频长度："+videoLenth);
        } catch (Exception e) {
            e.printStackTrace();
        }

        initM3u8();

        if(DownloadComplete(videoid)){
            System.out.println("===================完全下载了，就直接开始播放");
            finished = true;

            new Thread(){
                @Override
                public void run() {
                    writeCompleteM3u8();
                }
            }.start();

            System.out.println("开始播放");
            PlayerView playerView = findViewById(R.id.player_view);
            player = ExoPlayerFactory.newSimpleInstance(VideoPlayActivity.this);
            playerView.setPlayer(player);
            dataSourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(VideoPlayActivity.this, "ShareIt"));
            hlsMediaSource = new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.fromFile(m3u8file));
            player.setPlayWhenReady(true);
            player.prepare(hlsMediaSource);
        }else{ //没有完全下载下来，去网络中查找，并启动获取线程
//            searchVideoChunks();
            getChunkThread.start(); //如果没有下载完，就去并发下载播放就行了。
        }
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

    Handler handler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what){
                case 1:
                    playVideo();
            }
        }
    };

    public void playVideo(){
        //初始化播放器
        mProgressBar=findViewById(R.id.my_progress_bar);
        BandwidthMeter bandwidthMeter=new DefaultBandwidthMeter();
        TrackSelection.Factory trackSelectionFactory=new AdaptiveTrackSelection.Factory(bandwidthMeter);
        TrackSelector trackSelector = new DefaultTrackSelector(trackSelectionFactory);
        LoadControl loadControl = new DefaultLoadControl();
        player=ExoPlayerFactory.newSimpleInstance(this,trackSelector,loadControl);
        PlayerView playerView = findViewById(R.id.player_view);
        playerView.setPlayer(player);

        //设置数据源
        dataSourceFactory = new DefaultDataSourceFactory(VideoPlayActivity.this, Util.getUserAgent(VideoPlayActivity.this, "ShareIt"));
        hlsMediaSource = new HlsMediaSource.Factory(dataSourceFactory)
                .setAllowChunklessPreparation(true).createMediaSource(Uri.fromFile(m3u8file));
        player.setPlayWhenReady(true);
        player.prepare(hlsMediaSource);
        player.addListener(new MyEventListener());
    }

    public class MyEventListener implements Player.EventListener {
        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            System.out.println("===============事件："+playbackState);
            switch (playbackState){
                case ExoPlayer.STATE_ENDED: //4
                    //Stop playback and return to start position
                    player.seekTo(0);
                    break;
                case ExoPlayer.STATE_READY: //3
                    mProgressBar.setVisibility(View.GONE);
                    setProgress(0);
                    break;
                case ExoPlayer.STATE_BUFFERING: //2
                    mProgressBar.setVisibility(View.VISIBLE);
                    break;
                case ExoPlayer.STATE_IDLE: //1
                    break;
            }
        }
    }

    public class GetChunkThread extends Thread{
        @Override
        public void run() {
            int i=0;
            Model.VideoChunk v=null;
            while(!finished){
                String chunkName="out"+String.format("%04d", i)+".ts";
                System.out.println("===================chunkName+"+chunkName);
                try {
                    v=Textile.instance().videos.getVideoChunk(videoid, chunkName);
                    if (v == null) {
                        while(true){ //本地没有就一直去找，直到找到为止
                            v = Textile.instance().videos.getVideoChunk(videoid, chunkName);
                            if (v != null) //如果已经获取到了ts文件
                                break;
                            System.out.println("==========获取chunk："+chunkName);
                            if(!addressMap.containsKey(chunkName)) { //如果还没有ts的hash就去找hash
                                System.out.println("======gettinghash："+chunkName);
                                searchTheChunk(chunkName);
                            }else{
                                System.out.println("======获取到hash了："+chunkName);
                            }
                            try {
                                sleep(1500);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }else{
                        System.out.println("================获取到了chunk："+v.getChunk());
                    }
                    writeM3u8(v);
                    if(v.getEndTime()>=videoLenth - gap){ //如果是最后一个就终止,videolength是微秒，
                        System.out.println("==========末端差距："+v.getEndTime()+" "+(videoLenth - gap));
                        finished=true;
                        writeM3u8End();
                    }
                    if(ableToPlay() && player==null){
//                    if(i>4 && player==null){
                        System.out.println("=================开始播放了");
                        Message msg=new Message();
                        msg.what=1;
                        handler.sendMessage(msg);
                    }
                    i++; //处理下一个视频
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void writeCompleteM3u8(){
        int i=0;
        while(true){
            String chunkName="out"+String.format("%04d", i)+".ts";
            try {
//                Thread.sleep(10000);
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
        m3u8file=new File(dir+"/chunks/playlist.m3u8");
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
                    "#EXT-X-TARGETDURATION:5\n" +
                    "#EXT-X-PLAYLIST-TYPE:EVENT\n";
        try {
            fileWriter = new FileWriter(m3u8file);
            fileWriter.write(head);
            fileWriter.flush();
//            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("===========m3u8文件初始化");
    }

    public void writeM3u8(Model.VideoChunk v){
//        m3u8file=new File(dir+"/chunks/playlist.m3u8");
        int duration0=v.getEndTime()-v.getStartTime(); //微秒
        float size = (float)duration0/1000000;
        DecimalFormat df = new DecimalFormat("0.000000");//格式化小数，不足的补0
        String duration = df.format(size);//返回的是String类型的
        try {
//            FileWriter fileWriter = new FileWriter(m3u8file,true);
            String append = "#EXTINF:"+duration+",\n"+
                            v.getChunk()+"\n";
            fileWriter.write(append);
            fileWriter.flush();
//            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        m3u8WriteCount++;
        Log.d(TAG, "writeM3u8: 写次数："+m3u8WriteCount);
    }

    public void writeM3u8End(){
//        m3u8file=new File(dir+"/chunks/playlist.m3u8");
        try {
//            FileWriter fileWriter = new FileWriter(m3u8file,true);
            String append = "#EXT-X-ENDLIST";
            fileWriter.write(append);
            fileWriter.flush();
//            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
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

    private boolean ableToPlay(){
        System.out.println(m3u8WriteCount);
        if(m3u8WriteCount > 1 || finished)
            return true;
        return false;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void getAnResult(Model.VideoChunk videoChunk){
        addressMap.put(videoChunk.getChunk(),videoChunk.getAddress()); //拿到一个结果就放进来一个，可能会相同
        videorHelper.receiveChunk(videoChunk); //将对应的视频保存到本地
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop: VideoPlayActivity调用stop");
        if(EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().unregister(this);
        }

        videorHelper.stopReceiver();
        finished=true;

        //结束下载
        if(player!=null){
            player.release(); //释放播放器
        }
    }

}
