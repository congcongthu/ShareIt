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
import com.sjtuopennetwork.shareit.util.VideoHelper;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;

import sjtu.opennet.hon.Handlers;
import sjtu.opennet.hon.Textile;
import sjtu.opennet.honvideo.VideoReceiveHelper;
import sjtu.opennet.honvideo.VideoReceiver;
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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_play);

        if(!EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().register(this);
        }

        //查找本地chunk
//        Model.VideoChunk v=Textile.instance().videos.chunksById();

        //网络中查找
        searchVideoChunk();

        //比较

        int i=0;
//        if(getIntent().getBooleanExtra("ismine",true)){ //如果是我自己的视频，就直接播放文件
//            //播放本地m3u8
//            videoid=getIntent().getStringExtra("videoid");
//            String dir=VideoHelper.getVideoPathFromID(this,videoid);
//            PlayerView playerView=findViewById(R.id.player_view);
////            File m3u8file1= FileUtil.getM3u8FIle(dir);
//
////
////            try {
////                FileReader reader=new FileReader(m3u8file1);
////                LineNumberReader rl=new LineNumberReader(reader);
////                String tmp="";
////                while(tmp!=null){
////                    tmp=rl.readLine();
////                    System.out.println("==============读出一行："+tmp);
////                }
////                reader.close();
////                rl.close();
////            } catch (Exception e) {
////                e.printStackTrace();
////            }
//
//
//            File m3u8file=new File(dir+"/chunks/playlist.m3u8"); //新建一个
//            if(!m3u8file.exists()){ //如果不存在就创建一个（必然不存在）
//                try {
//                    m3u8file.createNewFile();
//                    //写入头部信息
//                    FileWriter fileWriter=new FileWriter(m3u8file); //写入器
//                    String head="#EXTM3U\n" +
//                            "#EXT-X-VERSION:3\n" +
//                            "#EXT-X-MEDIA-SEQUENCE:0\n" +
//                            "#EXT-X-ALLOW-CACHE:YES\n" +
//                            "#EXT-X-TARGETDURATION:15\n" +
//                            "#EXT-X-PLAYLIST-TYPE:EVENT\n"+
//                            "#EXTINF:10.914611,\n"+
//                            "out0000.ts\n"+
//                            "#EXT-X-ENDLIST\n";
//                    fileWriter.write(head);
//                    fileWriter.flush();
//                    fileWriter.close();
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//            new Thread(){
//                @Override
//                public void run() {
//                    try {
//                        Thread.sleep(5000);
//                        File m3u8file2=new File(dir+"/chunks/playlist2.m3u8"); //新建一个
//                        FileWriter fileWriter = new FileWriter(m3u8file2); //写新文件
//                        fileWriter.write("");
//                        fileWriter.flush();
//                        String head="#EXTM3U\n" +
//                                "#EXT-X-VERSION:3\n" +
//                                "#EXT-X-MEDIA-SEQUENCE:1\n" +
//                                "#EXT-X-ALLOW-CACHE:YES\n" +
//                                "#EXT-X-TARGETDURATION:15\n" +
//                                "#EXT-X-PLAYLIST-TYPE:EVENT\n"+
//                                "#EXTINF:10.914611,\n"+
//                                "out0000.ts\n"+
//                                "#EXTINF:9.907944,\n"+
//                                "out0001.ts\n"+
//                                "#EXT-X-ENDLIST\n";
//                        fileWriter.write(head);
//                        fileWriter.flush();
//                        fileWriter.close();
//                        HlsMediaSource hlsMediaSource1 =
//                                new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.fromFile(m3u8file2));
//                        mediaSource= new ConcatenatingMediaSource(hlsMediaSource,hlsMediaSource1);
////                        hlsMediaSource.
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//
////                    try {
////                        FileWriter fileWriter = new FileWriter(m3u8file); //写新文件
////                        fileWriter.write("");
////                        fileWriter.flush();
////                        String head="#EXTM3U\n" +
////                                "#EXT-X-VERSION:3\n" +
////                                "#EXT-X-MEDIA-SEQUENCE:0\n" +
////                                "#EXT-X-ALLOW-CACHE:YES\n" +
////                                "#EXT-X-TARGETDURATION:15\n" +
////                                "#EXT-X-PLAYLIST-TYPE:EVENT\n"+
////                                "#EXTINF:10.914611,\n"+
////                                "out0000.ts\n"+
////                                "#EXTINF:9.907944,\n"+
////                                "out0001.ts\n"+
////                                "#EXT-X-ENDLIST\n";
////                        fileWriter.write(head);
////                        fileWriter.close();
////                        Thread.sleep(5000);
////                    } catch (Exception e) {
////                        e.printStackTrace();
////                    }
////                    LineNumberReader reader = new LineNumberReader(fileReader);
////
////                    for(int i=0;i<3;i++){
////                        try {
////                            Thread.sleep(5000);
////                            //从原来的文件读出#EXTINF:10.977722, out0000.ts
////                            //就是第7+2*i行，8+2*i行。
////                            int extinf=7+2*i;
////                            String extinf_str="";
////                            int line=0;
////                            while(extinf_str!=null){
////                                line++;
////                                extinf_str=reader.readLine();
////                                if(line==extinf){ //如果到了这一行，就读出来，并append到m3u8中
////                                    System.out.println("===========读出一行:"+extinf_str);
////                                    fileWriter.append("\n"+extinf_str);
////                                    String tsname=reader.readLine();
////                                    fileWriter.append("\n"+tsname);
////                                }
////                            }
////                        } catch (Exception e) {
////                            e.printStackTrace();
////                        }
////                    }
//                }
//            }.start();
//
//
////            HlsMediaPlaylist hlsMediaPlaylist=new HlsMediaPlaylist();
//
//            player = ExoPlayerFactory.newSimpleInstance(this);
//            playerView.setPlayer(player);
//            dataSourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, "ShareIt"));
//            hlsMediaSource = new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.fromFile(m3u8file));
//            player.prepare(hlsMediaSource);
//
////            player.addListener(new MyEventListener());
//        }else{
//            videoid=getIntent().getStringExtra("videoid");
//            System.out.println("=============videoid别人发的："+videoid+ VideoHelper.getVideoPathFromID(this,videoid));
//
//            Model.Video video=null;
//            try {
//                video=Textile.instance().videos.getVideo(videoid);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            videorHelper=new VideoReceiveHelper(this,video);
//
//            //发出查询
//            searchVideoChunk();
//
//            //播放
//            play();
//
//        }

    }

    private void play() {

    }

    public  void searchVideoChunk(){
        QueryOuterClass.QueryOptions options = QueryOuterClass.QueryOptions.newBuilder()
                .setWait(100)
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

        videorHelper.receiveChunk(videoChunk);

    }

    @Override
    public void onStop() {
        super.onStop();

        if(EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().unregister(this);
        }
    }
}
