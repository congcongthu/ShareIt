package com.sjtuopennetwork.shareit.share;

import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.MediaController;
import android.widget.VideoView;

import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.sjtuopennetwork.shareit.R;
import com.sjtuopennetwork.shareit.util.FileUtil;
import com.sjtuopennetwork.shareit.util.VideoHelper;

import java.io.File;


public class VideoPlayActivity extends AppCompatActivity {

    VideoView playMyVideo;
//
//    public class MyServer extends NanoHTTPD {
//
//        public MyServer(int port) {
//            super(port);
//        }
//
//        @Override
//        public Response serve(IHTTPSession session) {
//            StringBuilder builder = new StringBuilder();
//            builder.append("<!DOCTYPE html><html><body>");
//            builder.append("Sorry, Can't Found the page!");
//            builder.append("</body></html>\n");
//            return newFixedLengthResponse(builder.toString());
//        }
//    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(getIntent().getBooleanExtra("ismine",true)){ //如果是我自己的视频，就直接播放文件
            setContentView(R.layout.activity_video_play);
//            playMyVideo=findViewById(R.id.play_my_video);

            //直接播放
            String videoid=getIntent().getStringExtra("videoid");
            String dir=VideoHelper.getVideoPathFromID(this,videoid);
            File m3u8file= FileUtil.getM3u8FIle(dir);


            SimpleExoPlayer player = ExoPlayerFactory.newSimpleInstance(this);
            PlayerView playerView=findViewById(R.id.player_view);
            playerView.setPlayer(player);
            DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this,
                    Util.getUserAgent(this, "ShareIt"));
            HlsMediaSource hlsMediaSource =
                    new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.fromFile(m3u8file));
            player.prepare(hlsMediaSource);



        }else{
            setContentView(R.layout.activity_video_play);
            String videoid=getIntent().getStringExtra("videoid");
            System.out.println("=============videoid别人发的："+videoid+ VideoHelper.getVideoPathFromID(this,videoid));

            //新线程根据id去ipfs上拿ts,



            //播放



        }
    }
}
