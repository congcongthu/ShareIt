package sjtu.opennet.stream.video;

import android.content.Context;
import android.net.Uri;
import android.os.Message;
import android.util.Log;

import com.googlecode.protobuf.format.JsonFormat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;

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

    public VideoGetter(Context context, String videoId){
        this.context=context;
        this.videoId=videoId;
    }

    public void startGet(){
        dir= FileUtil.getAppExternalPath(context, "video/"+videoId);
        m3u8file=M3U8Util.initM3u8(dir);
        Textile.instance().addEventListener(new VideoTsGetListener());
        try {
            Textile.instance().streams.subscribeStream(videoId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Uri getUri(){
        return Uri.parse("");
    }

    public void stopGet(){
        //close http

        //close and delete listener
    }

    class VideoTsGetListener extends BaseTextileEventListener {
        @Override
        public void notificationReceived(Model.Notification notification) {
            if(notification.getBody().equals("stream file")){
                Log.d(TAG, "notificationReceived: 收到streamfile");
                try {
                    View.VideoDescription.Builder b=View.VideoDescription.newBuilder();
                    JsonFormat.merge(notification.getSubjectDesc(),b);
                    View.VideoDescription videoDescription= b.build();
                    Log.d(TAG, "notificationReceived: "+ videoDescription);
                    Textile.instance().ipfs.dataAtPath(notification.getBlock(), new Handlers.DataHandler() {
                        @Override
                        public void onComplete(byte[] data, String media) {
                            Log.d(TAG, "onComplete: ======成功下载ts文件");
                            String tsName=dir + "/" + videoDescription.getChunk();
                            FileUtil.writeByteArrayToFile(tsName,data);
                            M3U8Util.writeM3u8(m3u8file,videoDescription.getEndTime(),videoDescription.getStartTime(),videoDescription.getChunk());
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
