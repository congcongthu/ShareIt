package sjtu.opennet.stream.video;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;

import java.io.File;

import sjtu.opennet.hon.Textile;
import sjtu.opennet.stream.util.FileUtil;
import sjtu.opennet.stream.util.Segmenter;
import sjtu.opennet.textilepb.Model;

public class VideoPusher {
    private static final String TAG = "HONVIDEO.VideoPusher";

    private String videoFilePath;
    private Context context;
    private String threadId;
    private VideoMeta videoMeta;
    private final int segmentTime = 3;

    private String videoRootDir;
    private String videoCacheDir;
    private String chunkDir;
    private String m3u8Path;

    private String command;

    private VideoStreamAddChunk videoStreamAddChunk;

    public VideoPusher(Context context, String threadId, String videoFilePath){
        this.context=context;
        this.threadId=threadId;
        this.videoFilePath=videoFilePath;

        // get metadata of the video with the inputted path
        String getVideoMetaCmd = String.format("-i %s -c copy -bsf:v h264_mp4toannexb -map 0 -f segment " +
                "-segment_time %d " +
                "-segment_list_size 1 ", videoFilePath,segmentTime);
        videoMeta=new VideoMeta(videoFilePath,getVideoMetaCmd.getBytes());

        // set the file paths
        String tmpPath = "video/"+videoMeta.getHash();
        videoCacheDir = FileUtil.getAppExternalPath(context, tmpPath);
        chunkDir = FileUtil.getAppExternalPath(context, tmpPath+"/chunks");
        m3u8Path = videoCacheDir+"/playlist.m3u8";

        videoMeta.saveThumbnail(videoCacheDir);

        command=String.format("-i %s -c copy -bsf:v h264_mp4toannexb -map 0 -f segment " +
                "-segment_time %d " +
                "-segment_list_size 10 " +
                "-segment_list %s " +
                "%s/out%%04d.ts", videoFilePath,segmentTime, m3u8Path, chunkDir);

        videoStreamAddChunk =new VideoStreamAddChunk(videoCacheDir,videoMeta.getHash());
    }

    public void startPush(){

        // startStream will sync the video message to thread
        Model.StreamMeta streamMeta= Model.StreamMeta.newBuilder().setId(videoMeta.getHash()).setNsubstreams(1).build();
        try {
            Textile.instance().streams.startStream(threadId,streamMeta);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // start to watch the videoCacheDir
        videoStreamAddChunk.start();

        // start to segment video
        new SegmentThread().start();

    }

    public String getVideoId(){return videoMeta.getHash();}

    public Bitmap getPosterBitmap() { return videoMeta.getPoster(); }

    private ExecuteBinaryResponseHandler segHandler=new ExecuteBinaryResponseHandler(){
        @Override
        public void onFinish() {
            Log.d(TAG, "onFinish: finish segmenting "+videoMeta.getHash());
            videoStreamAddChunk.finishAdd();
        }
    };

    class SegmentThread extends Thread{
        @Override
        public void run() {
            try {
                File outDirf = new File(chunkDir);
                if(!outDirf.exists()){
                    outDirf.mkdir();
                }
                Segmenter.segment(context, command, segHandler);
            } catch (Exception e) {
                Log.e(TAG, "Error occur when segment video.");
                e.printStackTrace();
            }
        }
    }

}
