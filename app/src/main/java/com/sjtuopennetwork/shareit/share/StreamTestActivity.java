package com.sjtuopennetwork.shareit.share;

import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.sjtuopennetwork.shareit.R;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;

import sjtu.opennet.hon.Handlers;
import sjtu.opennet.hon.Textile;
import sjtu.opennet.honvideo.VideoUploadHelper;
import sjtu.opennet.textilepb.View;

public class StreamTestActivity extends AppCompatActivity {
    private static final String TAG = "================";

    String streamid;
    String dir;
    HlsMediaSource hlsMediaSource;
    DataSource.Factory dataSourceFactory;
    SimpleExoPlayer player;
    MediaSource videoSource;
    File m3u8file;
    FileWriter fileWriter;
    PlayerView playerView;
    int tsCount;
    boolean notPlay;

    Handler handler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what){
                case 1:
                    playVideo();
                    break;
                case 2:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stream_test);

        boolean isMine=getIntent().getBooleanExtra("ismine",false);
        playerView = findViewById(R.id.player_view_1);

        if(isMine){ //自己的就直接播放本地视频
            String videoPath=getIntent().getStringExtra("videopath");
            //直接播放本地视频文件
            player = ExoPlayerFactory.newSimpleInstance(StreamTestActivity.this);
            playerView.setPlayer(player);
            dataSourceFactory = new DefaultDataSourceFactory(this,
                    Util.getUserAgent(this, "ShareIt"));
            videoSource = new ExtractorMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(Uri.parse(videoPath));
            player.prepare(videoSource);
        }else{ //他人的就订阅视频，并启动写m3u8线程和播放线程
            if(!EventBus.getDefault().isRegistered(this)){
                EventBus.getDefault().register(this);
            }
            tsCount=0;
            notPlay=true;

            streamid=getIntent().getStringExtra("streamid");
            try {
                Textile.instance().streams.subscribeStream(streamid);
                dir = VideoUploadHelper.getVideoPathFromID(this, streamid);
            } catch (Exception e) {
                e.printStackTrace();
            }

            //初始化m3u8和播放器
            initM3u8();
            initPlayer();
        }

    }

    public void initPlayer(){
        BandwidthMeter bandwidthMeter=new DefaultBandwidthMeter();
        TrackSelection.Factory trackSelectionFactory=new AdaptiveTrackSelection.Factory(bandwidthMeter);
        TrackSelector trackSelector = new DefaultTrackSelector(trackSelectionFactory);
        LoadControl loadControl = new DefaultLoadControl();
        player=ExoPlayerFactory.newSimpleInstance(StreamTestActivity.this,trackSelector,loadControl);
        playerView.setPlayer(player);
    }

    public void playVideo(){
        notPlay=false;

        dataSourceFactory = new DefaultDataSourceFactory(StreamTestActivity.this, Util.getUserAgent(StreamTestActivity.this, "ShareIt"));
        hlsMediaSource = new HlsMediaSource.Factory(dataSourceFactory)
                .setAllowChunklessPreparation(true).createMediaSource(Uri.fromFile(m3u8file));
        player.prepare(hlsMediaSource);
    }

    public void initM3u8(){
        String head="#EXTM3U\n" +
                "#EXT-X-VERSION:3\n" +
                "#EXT-X-MEDIA-SEQUENCE:0\n" +
                "#EXT-X-ALLOW-CACHE:YES\n" +
                "#EXT-X-TARGETDURATION:5\n" +
                "#EXT-X-PLAYLIST-TYPE:EVENT\n";
        m3u8file=new File(dir+"/playlist.m3u8");

        try{
            fileWriter=new FileWriter(m3u8file);
            fileWriter.write(head);
            fileWriter.flush();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void writeM3u8(long end,long start, String chunkName){
        long duration0=end-start; //微秒
        double size = (double)duration0/1000000;
        DecimalFormat df = new DecimalFormat("0.000000");//格式化小数，不足的补0
        String duration = df.format(size);//返回的是String类型的
        String append = "#EXTINF:"+duration+",\n"+
                chunkName+"\n";
        try {
            fileWriter.write(append);
            fileWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        tsCount++;
        if(tsCount>1 && notPlay){
            Log.d(TAG, "writeM3u8: 启动播放器");
            Message msg=new Message();
            msg.what=1;
            handler.sendMessage(msg);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void getTs(Pair<String, View.VideoDescription> p){
        Log.d(TAG, "getTs: "+p.first+" "+p.second.getChunk());

        Textile.instance().ipfs.dataAtPath(p.first, new Handlers.DataHandler() {
            @Override
            public void onComplete(byte[] data, String media) {
                Log.d(TAG, "onComplete: ======成功下载ts文件");

                try {
                    File file = new File(dir + "/" + p.second.getChunk());
                    FileOutputStream o = new FileOutputStream(file);
                    o.write(data);
                }catch (Exception e){
                    e.printStackTrace();
                }

                writeM3u8(p.second.getEndTime(),p.second.getStartTime(),p.second.getChunk());
            }

            @Override
            public void onError(Exception e) {

            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        if(EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().unregister(this);
        }
    }

}
