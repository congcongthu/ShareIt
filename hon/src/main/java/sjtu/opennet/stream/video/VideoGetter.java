package sjtu.opennet.stream.video;

import android.content.Context;
import android.net.Uri;

import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.googlecode.protobuf.format.JsonFormat;

import java.io.File;
import sjtu.opennet.hon.BaseTextileEventListener;
import sjtu.opennet.hon.Handlers;
import sjtu.opennet.hon.Textile;
import sjtu.opennet.stream.util.FileUtil;
import sjtu.opennet.textilepb.Model;
import sjtu.opennet.textilepb.View;

public class VideoGetter {
    private static final String TAG = "HONVIDEO.VideoGetter_tkt";
    Context context;
    String videoId;
    String dir;

    File m3u8file;
    VideoTsGetListener videoTsGetListener;

    public VideoGetter(Context context, String videoId){
        this.context=context;
        this.videoId=videoId;

        videoTsGetListener=new VideoTsGetListener();
    }

    public void startGet(){
        dir= FileUtil.getAppExternalPath(context, "tsFile");
        m3u8file=M3U8Util.initM3u8(dir,videoId);
        Textile.instance().addEventListener(videoTsGetListener);
        try {
            Textile.instance().streams.subscribeStream(videoId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Uri getUri(){
        return Uri.fromFile(m3u8file);
    }

    public void stopGet(){
        //close and delete listener
        Textile.instance().removeEventListener(videoTsGetListener);
    }

    class VideoTsGetListener extends BaseTextileEventListener {
        @Override
        public void notificationReceived(Model.Notification notification) {
            if(notification.getBody().equals("stream file")){
                Log.d(TAG, "notificationReceived: 收到streamfile");
                if(notification.getBlock().equals("")){
                    Log.d(TAG, "notificationReceived: get endflag");
                    M3U8Util.writeM3u8End(m3u8file);
                    return;
                }
                try {
//                    View.VideoDescription.Builder b=View.VideoDescription.newBuilder();
//                    JsonFormat.merge(notification.getSubjectDesc(),b);
                    JSONObject object= JSON.parseObject(notification.getSubjectDesc());
                    Log.d(TAG, "notificationReceived: "+ notification.getSubjectDesc());
                    Textile.instance().ipfs.dataAtPath(notification.getBlock(), new Handlers.DataHandler() {
                        @Override
                        public void onComplete(byte[] data, String media) {
                            Log.d(TAG, "onComplete: ======成功下载ts文件");
                            String tsName=dir + "/" + notification.getBlock();
                            FileUtil.writeByteArrayToFile(tsName,data);
//                            long starttime=Long.parseLong(object.getString("startTime"));
//                            long endtime=Long.parseLong(object.getString("endTime"));
                            long starttime=object.getLongValue("startTime");
                            long endtime=object.getLongValue("endTime");
                            Log.d(TAG, "onComplete: starttime endtime: "+starttime+" "+endtime);
                            M3U8Util.writeM3u8(m3u8file,endtime,starttime,notification.getBlock());
                        }
                        @Override
                        public void onError(Exception e) {
                            e.printStackTrace();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
