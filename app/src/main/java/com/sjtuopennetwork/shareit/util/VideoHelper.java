package com.sjtuopennetwork.shareit.util;
import android.content.Context;
import android.util.Log;

import java.io.File;

import sjtu.opennet.hon.Handlers;
import sjtu.opennet.hon.Textile;
import sjtu.opennet.honvideo.Segmenter;
import sjtu.opennet.honvideo.VideoMeta;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.sjtuopennetwork.shareit.util.FileUtil;
import sjtu.opennet.textilepb.Model.Video;

/**
 *
 */
public class VideoHelper {
    private static final String TAG = "VideoHelper";
    private VideoMeta vMeta;
    private String rootPath;
    private String videoPath;
    private String chunkPath;
    private String thumbPath;
    private String m3u8Path;
    private VideoFileListener vObserver;
    private Context context;
    private String filePath;
    private Video videoPb;

    private ExecuteBinaryResponseHandler segHandler = new ExecuteBinaryResponseHandler(){
        @Override
        public void onSuccess(String message) {
            Log.d(TAG, String.format("FFmpeg segment success\n%s", message));
        }

        @Override
        public void onProgress(String message){
            //Log.d(TAG, String.format("command get %s at %d.", message, System.currentTimeMillis()));
        }

        @Override
        public void onFailure(String message) {
            Log.e(TAG, "Command failure.");
            vObserver.stopWatching();
        }

        @Override
        public void onStart() {
            Log.d(TAG, "FFmpeg segment start.");
            vObserver.startWatching();
        }

        @Override
        public void onFinish() {
            vObserver.stopWatching();
        }
    };

    private Handlers.IpfsAddDataHandler posterHandler = new Handlers.IpfsAddDataHandler() {
        @Override
        public void onComplete(String path) {
            Log.d(TAG, String.format("poster ipfs path: %s", path));
            videoPb = vMeta.getPb(path);
        }

        @Override
        public void onError(Exception e) {
            e.printStackTrace();
        }
    };

    public VideoHelper(Context context, String filePath){
        this.context = context;
        this.filePath = filePath;
        vMeta = new VideoMeta(filePath);
        getVideoPb();
        rootPath = FileUtil.getAppExternalPath(context,"video");
        String tmpPath = String.format("video/%s", vMeta.getHash());
        videoPath = FileUtil.getAppExternalPath(context, tmpPath);
        tmpPath = String.format("%s/chunks", tmpPath);
        chunkPath = FileUtil.getAppExternalPath(context, tmpPath);

        thumbPath = vMeta.saveThumbnail(videoPath);
        m3u8Path = String.format("%s/playlist.m3u8", videoPath);

        vObserver = new VideoFileListener(chunkPath);
    }

    public void segment(){
        try {
            Segmenter.segment(context, 10, filePath, m3u8Path, chunkPath, segHandler);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void publishMeta(){
        try {
            Textile.instance().videos.addVideo(videoPb);
            Textile.instance().videos.publishVideo(videoPb);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public static void cleanAll(Context context){
        String rootPath = FileUtil.getAppExternalPath(context, "video");
        File rmFile = new File(rootPath);
        FileUtil.deleteContents(rmFile);
    }

    private void getVideoPb(){
        Textile.instance().ipfs.ipfsAddData(vMeta.getPosterByte(),true,false,posterHandler);

        //Textile.instance().ipfs.ipfsAddData();
        //Textile.instance().ipfs.addObject;
    }
}
