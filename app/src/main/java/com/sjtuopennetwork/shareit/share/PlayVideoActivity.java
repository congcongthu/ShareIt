package com.sjtuopennetwork.shareit.share;

import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.sjtuopennetwork.shareit.R;

import sjtu.opennet.stream.video.VideoGetter;
import sjtu.opennet.stream.video.ticketvideo.VideoGetter_tkt;


public class PlayVideoActivity extends AppCompatActivity {

    PlayerView playerView;
    DataSource.Factory dataSourceFactory;
    SimpleExoPlayer player;
    VideoGetter videoGetter;
    VideoGetter_tkt videoGetterTkt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_stream_play);

        boolean isMine=getIntent().getBooleanExtra("ismine",false);
        playerView = findViewById(R.id.video_stream_player_view);

        player = ExoPlayerFactory.newSimpleInstance(PlayVideoActivity.this);
        playerView.setPlayer(player);

        if(isMine){ //自己的就直接播放本地视频
            Uri uri=Uri.parse(getIntent().getStringExtra("videopath"));
            //直接播放本地视频文件
            dataSourceFactory = new DefaultDataSourceFactory(this,
                    Util.getUserAgent(this, "ShareIt"));
            MediaSource videoSource = new ExtractorMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(uri);
            player.prepare(videoSource);
        }else{ //播放m3u8文件，根据videoid来构建m3u8的地址
            String videoId=getIntent().getStringExtra("videoid");
            boolean ticket=getIntent().getBooleanExtra("ticket",false);
            if(ticket){
                videoGetterTkt=new VideoGetter_tkt(this,videoId);
                videoGetterTkt.startGet();
            }else{ //不是ticket
                videoGetter =new VideoGetter(this,videoId);
                videoGetter.startGet();
            }

//            Uri uri=videoGetter.getUri();
//            dataSourceFactory =
//                    new DefaultHttpDataSourceFactory(Util.getUserAgent(this, "ShareIt"));
//            HlsMediaSource hlsMediaSource=
//                    new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
//            player.prepare(hlsMediaSource);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if(videoGetter !=null){
            videoGetter.stopGet();
        }

        if(videoGetterTkt!=null){
            videoGetterTkt.stopGet();
        }

        //结束播放
        if(player!=null){
            player.stop();
            player.release(); //释放播放器
        }
    }
}
