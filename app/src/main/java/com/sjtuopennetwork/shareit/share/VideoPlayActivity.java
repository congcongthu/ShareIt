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
    ConcatenatingMediaSource mediaSource;
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

    //两个线程
//    GetChunkThread getChunkThread=new GetChunkThread();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_play);

        boolean isMine=getIntent().getBooleanExtra("ismine",false);
        if(isMine){ //如果是我自己的
            String videoPath=getIntent().getStringExtra("videopath");
            Log.d(TAG, "onCreate: 播放自己发的视频："+videoPath);
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

            m3u8WriteCount=0;
            addressMap = new HashMap<>();

//            if(!EventBus.getDefault().isRegistered(this)){
//                EventBus.getDefault().register(this);
//            }

            videoid = getIntent().getStringExtra("videoid");

            try {
                video = Textile.instance().videos.getVideo(videoid);
//                videorHelper = new VideoReceiveHelper(this, video);

                notplayed=true;
                videorHelper=new VideoReceiveHelper(this, video, new VideoHandlers.ReceiveHandler() {
                    @Override
                    public void onChunkComplete(Model.VideoChunk vChunk) {
                        Log.d(TAG, "onChunkComplete: 写m3u8"+m3u8WriteCount);
                        writeM3u8(vChunk);
                        if((m3u8WriteCount > 1 || finished) && notplayed){ //写了3次就可以播放
                            Log.d(TAG, "onChunkComplete: 开始播放");
                            Message msg=new Message();
                            msg.what=1;
                            handler.sendMessage(msg);
                        }
                    }

                    @Override
                    public void onVideoComplete() {
                        writeM3u8End();
                    }

                    @Override
                    public void onError(Exception e) {

                    }
                });

                dir = VideoUploadHelper.getVideoPathFromID(this, videoid);
                videoLength = video.getVideoLength();
                Log.d(TAG, "onCreate: 视频长度："+videoLength);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if(DownloadComplete(videoid)){
                finished = true;

                //读取m3u8文件
                m3u8file=new File(dir+"/chunks/playlist.m3u8");

                PlayerView playerView = findViewById(R.id.player_view);
                player = ExoPlayerFactory.newSimpleInstance(VideoPlayActivity.this);
                playerView.setPlayer(player);
                dataSourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(VideoPlayActivity.this, "ShareIt"));
                hlsMediaSource = new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.fromFile(m3u8file));
                player.setPlayWhenReady(true);
                player.prepare(hlsMediaSource);
            }else{ //没有完全下载下来，去网络中查找，并启动获取线程
//            searchVideoChunks();
                initM3u8();
                finished=false;

                videorHelper.downloadVideo();

                //初始化播放器
                mProgressBar=findViewById(R.id.my_progress_bar);
                BandwidthMeter bandwidthMeter=new DefaultBandwidthMeter();
                TrackSelection.Factory trackSelectionFactory=new AdaptiveTrackSelection.Factory(bandwidthMeter);
                TrackSelector trackSelector = new DefaultTrackSelector(trackSelectionFactory);
                LoadControl loadControl = new DefaultLoadControl();
                player=ExoPlayerFactory.newSimpleInstance(this,trackSelector,loadControl);
                PlayerView playerView = findViewById(R.id.player_view);
                playerView.setPlayer(player);
//                getChunkThread.start(); //如果没有下载完，就去并发下载播放就行了。

                //播放
//                playVideo();

            }
        }
    }

    public static boolean DownloadComplete(String vid){
//        List<Model.VideoChunk> tmp;
//        try {
//            tmp = Textile.instance().videos.chunksByVideoId(vid).getItemsList();
//        } catch (Exception e) {
//            tmp = new ArrayList<>();
//        }
//        if(tmp.size() == 0) {
//            System.out.println("no local chunks");
//            return false;
//        }
//        long maxEndtime = 0;
//        String lastChunk = "";
//        for (Model.VideoChunk c : tmp){
//            if (c.getEndTime() > maxEndtime) {
//                maxEndtime = c.getEndTime();
//                lastChunk = c.getChunk();
//            }
//        }
//        System.out.println("max end time: "+ maxEndtime);
//
//        try {
//            Model.Video tmpVideo = Textile.instance().videos.getVideo(vid);
//            if (tmpVideo == null) {
//                System.out.println("no local video");
//                return false;
//            }
//            int id = Segmenter.getIndFromPath(lastChunk);
//            System.out.println("video length:"+tmpVideo.getVideoLength());
//            System.out.println(tmp.size());
//            if (maxEndtime >= tmpVideo.getVideoLength()- gap && id == tmp.size()-1){
//                System.out.println("download complete!=================");
//                return true;
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return false;

        Model.VideoChunk v=null;
        try {
            v=Textile.instance().videos.getVideoChunk(vid,VideoHandlers.chunkEndTag);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        if(v==null){
            Log.d(TAG, "DownloadComplete: not completed");
            return false;
        }
        Log.d(TAG, "DownloadComplete: already completed");
        return true;
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
        notplayed=false;

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

    public class SearchChunk extends Thread{
        public String chunkName;
        public boolean searchFinish;

        public SearchChunk(String chunkName){
            this.chunkName=chunkName;
            searchFinish=false;
        }

        @Override
        public void run() {
            while (!searchFinish){
                searchTheChunk(chunkName);
                try {
                    sleep(1500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
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
                try {
                    v=Textile.instance().videos.getVideoChunk(videoid, chunkName);
                    if (v == null) {
                        SearchChunk searchChunk=new SearchChunk(chunkName);
                        searchChunk.start();
                        while(!finishGetHash){ //本地没有就一直去找，直到找到为止
                            v = Textile.instance().videos.getVideoChunk(videoid, chunkName);
                            if (v != null) { //如果已经获取到了ts文件
                                searchChunk.searchFinish = true;
                                break;
                            }
                            if(addressMap.containsKey(chunkName)) { //如果还没有ts的hash就去找hash
                                searchChunk.searchFinish=true;
                            }
                        }
                    }
                    writeM3u8(v);
                    if(v.getEndTime()>=videoLength - gap){ //如果是最后一个就终止,videolength是微秒，
                        finished=true;
                        writeM3u8End();
                    }
                    if((m3u8WriteCount > 2 || finished) && notplayed){

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
        try {
            long duration0=v.getEndTime()-v.getStartTime(); //微秒
            double size = (double)duration0/1000000;
            DecimalFormat df = new DecimalFormat("0.000000");//格式化小数，不足的补0
            String duration = df.format(size);//返回的是String类型的
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
        Log.d(TAG, "writeM3u8End: ");
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

//    @Subscribe(threadMode = ThreadMode.MAIN)
//    public void getAnResult(Model.VideoChunk videoChunk){
//        Log.d(TAG, "getAnResult: 得到videoChunk结果");
//        if(videoChunk.getId().equals(videoid)){
//            addressMap.put(videoChunk.getChunk(),videoChunk.getAddress()); //拿到一个结果就放进来一个，可能会相同
//            videorHelper.receiveChunk(videoChunk); //将对应的视频保存到本地
//        }
//    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop: VideoPlayActivity调用stop");

//        if(EventBus.getDefault().isRegistered(this)){
//            EventBus.getDefault().unregister(this);
//        }

        finished=true;
        finishGetHash=true;

        if(fileWriter!=null){
            try {
                fileWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

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
