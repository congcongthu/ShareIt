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
    FileWriter fileWriter;
    int tsCount;

    public VideoGetter(Context context, String videoId){
        this.context=context;
        this.videoId=videoId;
    }

    public void startGet(){
        dir= FileUtil.getAppExternalPath(context, "video/"+videoId);
        initM3u8();
        Textile.instance().addEventListener(new VideoTsGetListener());
        try {
            Textile.instance().streams.subscribeStream(videoId);
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    public synchronized void writeM3u8(long end,long start, String chunkName){
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
                            try {
                                File file = new File(dir + "/" + videoDescription.getChunk());
                                FileOutputStream o = new FileOutputStream(file);
                                o.write(data);
                            }catch (Exception e){
                                e.printStackTrace();
                            }
                            writeM3u8(videoDescription.getEndTime(),videoDescription.getStartTime(),videoDescription.getChunk());
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
