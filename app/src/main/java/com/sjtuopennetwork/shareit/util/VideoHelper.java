package com.sjtuopennetwork.shareit.util;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import sjtu.opennet.hon.Handlers;
import sjtu.opennet.hon.Textile;
import sjtu.opennet.honvideo.Segmenter;
import sjtu.opennet.honvideo.VideoMeta;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.sjtuopennetwork.shareit.util.FileUtil;
import sjtu.opennet.textilepb.Model.Video;
import org.apache.commons.io.input.Tailer;


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
    private File m3u8;
    private VideoFileListener vObserver;    //Observer to listen the close of ts chunk file.
    private boolean tailerRunning;

    private Tailer tailer;
    private  VideoListListener vListObserver;

    private Context context;
    private String filePath;
    private Video videoPb;

    static private Bitmap bitmapFromIpfs;

    private ExecuteBinaryResponseHandler segHandler = new ExecuteBinaryResponseHandler(){
        @Override
        public void onSuccess(String message) {
            Log.d(TAG, String.format("FFmpeg segment success\n%s", message));
            vObserver.stopWatching();
            tailer.stop();
            tailerRunning = false;
        }

        @Override
        public void onProgress(String message){
            //Log.d(TAG, String.format("command get %s at %d.", message, System.currentTimeMillis()));
            if(!m3u8.exists()){
                Log.d(TAG, String.format("%s does not exists", m3u8Path));
            }
            if(!tailerRunning && m3u8.exists()){
                tailer = new Tailer(m3u8, vListObserver, 200);
                tailerRunning = true;
                tailer.run();
            }
        }

        @Override
        public void onFailure(String message) {
            Log.e(TAG, "Command failure.");
            vObserver.stopWatching();
            tailer.stop();
            tailerRunning = false;
        }

        @Override
        public void onStart() {
            Log.d(TAG, "FFmpeg segment start.");
            vObserver.startWatching();



            //tailer = new Tailer(m3u8, vListObserver, 200);
            //tailer.run();
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

    static private Handlers.DataHandler posterReceiveHandler = new Handlers.DataHandler() {
        @Override
        public void onComplete(byte[] data, String media) {
            Log.d(TAG, String.format("Received media: %s", media));
            bitmapFromIpfs = BitmapFactory.decodeByteArray(data,0,data.length);
        }

        @Override
        public void onError(Exception e) {

        }
    };

    public VideoHelper(Context context, String filePath){
        this.context = context;
        this.filePath = filePath;
        vMeta = new VideoMeta(filePath);
        setVideoPb();
        rootPath = FileUtil.getAppExternalPath(context,"video");
        String tmpPath = String.format("video/%s", vMeta.getHash());
        videoPath = FileUtil.getAppExternalPath(context, tmpPath);
        tmpPath = String.format("%s/chunks", tmpPath);
        chunkPath = FileUtil.getAppExternalPath(context, tmpPath);

        thumbPath = vMeta.saveThumbnail(videoPath);
        m3u8Path = String.format("%s/playlist.m3u8", videoPath);

        m3u8 = new File(m3u8Path);
        vObserver = new VideoFileListener(chunkPath);
        vListObserver = new VideoListListener();

        tailerRunning = false;
    }

    public void segment(){
        try {
            Segmenter.segment(context, 10, filePath, m3u8Path, chunkPath, segHandler);
            //Segmenter.segment(context, 10, filePath, m3u8Path, chunkPath, null);
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

    /**
     * setVideoPb will add poster to ipfs and use the ipfs hash path to create video protobuf object.
     * It will called when create the new helper instance and assigned protobuf object to variable videoPb.
     */
    private void setVideoPb(){
        Textile.instance().ipfs.ipfsAddData(vMeta.getPosterByte(),true,false,posterHandler);

        //Textile.instance().ipfs.ipfsAddData();
        //Textile.instance().ipfs.addObject;
    }

    /**
     * Error Report:
     *      VideoHelper.getPosterFromPb may return null bitmap.
     *      That is because the bitmap returned is a static private value from VideoHelper.
     *      In that case, this bitmap may be returned before it was assigned.
     */
    public static Bitmap getPosterFromPb(Video vpb){
        String ipfsHash = vpb.getPoster();
        Textile.instance().ipfs.dataAtPath(ipfsHash, posterReceiveHandler);
        return bitmapFromIpfs;
    }

    public static void saveBitmap(Bitmap bitmapToSave, String outPath){
        try {
            File img = new File(outPath);
            OutputStream fout = new FileOutputStream(img);
            bitmapToSave.compress(Bitmap.CompressFormat.PNG, 100, fout);
            fout.flush();
            fout.close();
        }catch(FileNotFoundException fe){
            Log.e(TAG, "File not found");
            fe.printStackTrace();
        }catch(IOException ie){
            Log.e(TAG, "ioerror");
            ie.printStackTrace();
        }catch(NullPointerException ne){
            Log.e(TAG, "Bitmap is NULL.");
            ne.printStackTrace();
        }
    }

    public Video getVideoPb(){
        return videoPb;
    }
}
