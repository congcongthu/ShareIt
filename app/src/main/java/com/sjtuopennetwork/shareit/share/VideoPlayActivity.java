package com.sjtuopennetwork.shareit.share;

import android.content.pm.ActivityInfo;
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
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.hls.playlist.HlsMediaPlaylist;
import com.google.android.exoplayer2.source.hls.playlist.HlsPlaylist;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
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
import sjtu.opennet.honvideo.VideoHandlers;
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
    long videoLength;
    File m3u8file;
    String dir;
    int m3u8WriteCount;
    Model.Video video;
    boolean finished;
    static int gap = 200000;
    Map<String, String> addressMap;
    private ProgressBar mProgressBar;
    FileWriter fileWriter;
    MediaSource videoSource;
    boolean notplayed;
    boolean finishGetHash=false;
    private long NextChunk = 0;
    int rotation;
    int videoWidth;
    int videoHeight;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_play);
        finished =false;
        NextChunk = 0;

        boolean isMine=getIntent().getBooleanExtra("ismine",false);
        videoid = getIntent().getStringExtra("videoid");
        try {
            video = Textile.instance().videos.getVideo(videoid);
        } catch (Exception e) {
            e.printStackTrace();
        }
        dir = VideoUploadHelper.getVideoPathFromID(this, videoid);
        videoLength = video.getVideoLength();
        notplayed=true;
        rotation=video.getRotation();
        videoWidth=video.getWidth();
        videoHeight=video.getHeight();
        m3u8WriteCount=0;

        Log.d(TAG, "onCreate: rotation: "+rotation);

        if(isMine){
            String videoPath=getIntent().getStringExtra("videopath");

            Log.d(TAG, "initPlayer: rotation width height:"+rotation+" "+videoWidth+" "+videoHeight);
            if(rotation==0){
                Log.d(TAG, "initPlayer: 即将横屏播放");
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }

            //直接播放本地视频文件
            PlayerView playerView = findViewById(R.id.player_view);
            player = ExoPlayerFactory.newSimpleInstance(VideoPlayActivity.this);
            playerView.setPlayer(player);
            dataSourceFactory = new DefaultDataSourceFactory(this,
                    Util.getUserAgent(this, "ShareIt"));
            videoSource = new ExtractorMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(Uri.parse(videoPath));
            player.prepare(videoSource);

        }else{

            videorHelper=new VideoReceiveHelper(this, video, new VideoHandlers.ReceiveHandler() {
                    @Override
                    public void onChunkComplete(Model.VideoChunk vChunk) {
                        Log.d(TAG, "onChunkComplete: 写m3u8 "+m3u8WriteCount+" "+vChunk.getChunk());

                        //把可能的补充起来
                        long index = vChunk.getIndex();
                        Log.d(TAG, "onChunkComplete: intdex nextChunk "+index+" "+NextChunk);
                        for (long cur = NextChunk; cur < index; cur++){
                            try {
                                Model.VideoChunk v = Textile.instance().videos.getVideoChunk(videoid, cur);
                                writeM3u8(v);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        writeM3u8(vChunk);
                        NextChunk = index+1;
                        if((notplayed && (m3u8WriteCount > 0 || finished)) ){ //写了3次就可以播放
                            Log.d(TAG, "onChunkComplete: 开始播放");
                            Message msg=new Message();
                            msg.what=1;
                            handler.sendMessage(msg);
                        }
                    }

                    @Override
                    public void onVideoComplete() {
//
                    }

                    @Override
                    public void onError(Exception e) {

                    }
                });

            initM3u8();

            if(finished){
                initPlayer();
                playVideo();
            } else{
                videorHelper.downloadVideo();
                initPlayer();
            }
        }
    }

    Handler handler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what){
                case 1:
                    playVideo();
                    break;
            }
        }
    };

    public void initPlayer(){

        mProgressBar=findViewById(R.id.my_progress_bar); //环形进度条
        BandwidthMeter bandwidthMeter=new DefaultBandwidthMeter();
        TrackSelection.Factory trackSelectionFactory=new AdaptiveTrackSelection.Factory(bandwidthMeter);
        TrackSelector trackSelector = new DefaultTrackSelector(trackSelectionFactory);
        LoadControl loadControl = new DefaultLoadControl();
        player=ExoPlayerFactory.newSimpleInstance(VideoPlayActivity.this,trackSelector,loadControl);
        player.seekTo(0);
        PlayerView playerView = findViewById(R.id.player_view);
        Log.d(TAG, "initPlayer: rotation width height:"+rotation+" "+videoWidth+" "+videoHeight);
//        if(rotation==0){
        if(videoWidth>videoHeight){
            Log.d(TAG, "initPlayer: 即将横屏播放");
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        playerView.setPlayer(player);
    }

    public void playVideo(){
        notplayed=false;

        //设置数据源
        dataSourceFactory = new DefaultDataSourceFactory(VideoPlayActivity.this, Util.getUserAgent(VideoPlayActivity.this, "ShareIt"));
        hlsMediaSource = new HlsMediaSource.Factory(dataSourceFactory)
                .setAllowChunklessPreparation(true).createMediaSource(Uri.fromFile(m3u8file));
        player.setPlayWhenReady(true);
        player.prepare(hlsMediaSource);
        player.addListener(new MyEventListener());
        player.seekTo(0);
    }

    public class MyEventListener implements Player.EventListener {
        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            switch (playbackState){
                case ExoPlayer.STATE_ENDED: //4
                    //Stop playback and return to start position
                    player.seekTo(0);
                    break;
                case ExoPlayer.STATE_READY: //3
                    mProgressBar.setVisibility(View.GONE);
                    player.setPlayWhenReady(true);
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

    public void initM3u8(){
        String head="#EXTM3U\n" +
                "#EXT-X-VERSION:3\n" +
                "#EXT-X-MEDIA-SEQUENCE:0\n" +
                "#EXT-X-ALLOW-CACHE:YES\n" +
                "#EXT-X-TARGETDURATION:2\n" +
                "#EXT-X-PLAYLIST-TYPE:EVENT\n";

        m3u8file=new File(dir+"/chunks/playlist.m3u8");
        try{
            fileWriter = new FileWriter(m3u8file);
            fileWriter.write(head);
            fileWriter.flush();
            fileWriter.close();

            //补充已经下载过的
            boolean finishComplement=false;
            while(!finishComplement){
                try {
                    Model.VideoChunk v=Textile.instance().videos.getVideoChunk(videoid, NextChunk);
                    if ( v!=null ){ //本地有了，就写m3u8文件
                        Log.d(TAG, "从数据库读出来："+v.getChunk());
                        writeM3u8(v);
                        NextChunk++; //处理下一个视频
                    } else {
                        Log.d(TAG, "initM3u8：补充结束");
                        finishComplement=true;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    break;
                }
            }

        }catch (Exception e){
            e.printStackTrace();
        }
        System.out.println("===========m3u8文件初始化完毕");
    }

    public void writeM3u8(Model.VideoChunk v){
        if(v.getChunk().equals(VideoHandlers.chunkEndTag)){
            writeM3u8End();
            finished = true;
            return;
        }
        try {
            long duration0=v.getEndTime()-v.getStartTime(); //微秒
            double size = (double)duration0/1000000;
            DecimalFormat df = new DecimalFormat("0.000000");//格式化小数，不足的补0
            String duration = df.format(size);//返回的是String类型的
            FileWriter fileWriter = new FileWriter(m3u8file,true);
            String append = "#EXTINF:"+duration+",\n"+
                    v.getChunk()+"\n";
            fileWriter.write(append);
//            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        m3u8WriteCount++;
        Log.d(TAG, "writeM3u8: 写次数："+m3u8WriteCount);
    }

    public void writeM3u8End(){
        Log.d(TAG, "writeM3u8End: ");
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

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop: VideoPlayActivity调用stop");

        finished=true;
        finishGetHash=true;

        if(videorHelper!=null){
            videorHelper.stopReceiver();
        }

        //结束下载
        if(player!=null){
            player.stop();
            player.release(); //释放播放器
        }
    }
}
