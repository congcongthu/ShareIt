package sjtu.opennet.stream.video.ticketvideo;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;

import java.io.File;

import sjtu.opennet.hon.Handlers;
import sjtu.opennet.hon.Textile;
import sjtu.opennet.stream.util.FileUtil;
import sjtu.opennet.stream.util.Segmenter;
import sjtu.opennet.stream.video.VideoMeta;
import sjtu.opennet.textilepb.Model;

public class VideoSender_tkt {
    private static final String TAG = "=======================HONVIDEO.VideoSender_tkt";

    Context context;
    String videoFilePath;
    VideoMeta videoMeta;
    String threadId;

    private final int segmentTime = 3;

    private String videoCacheDir;
    private String chunkDir;
    private String m3u8Path;

    private String command;

    VideoAddChunk_tkt videoAddChunk;

    public VideoSender_tkt(Context context, String threadId, String videoFilePath) {
        this.context = context;
        this.threadId = threadId;
        this.videoFilePath = videoFilePath;

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

        videoAddChunk=new VideoAddChunk_tkt(videoCacheDir,videoMeta.getHash());
    }

    public void startSend(){
        Log.d(TAG, "startSend: tkt start to send");
        Textile.instance().ipfs.ipfsAddData(videoMeta.getPosterByte(), true, false, new Handlers.IpfsAddDataHandler() {
            @Override
            public void onComplete(String path) {
                Log.d(TAG, "onComplete: tkt poster hash: "+path);
                Model.Video videoPb=videoMeta.getPb(path);
                try {
                    Textile.instance().videos.addVideo(videoPb);
                    Textile.instance().videos.publishVideo(videoPb,false);
                    Textile.instance().videos.threadAddVideo(threadId,videoMeta.getHash());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
            }
        });

        videoAddChunk.start();
        new SegmentThread().start();
    }

    public String getVideoId(){return videoMeta.getHash();}

    public Bitmap getPosterBitmap() { // temporarily not add the poster to ipfs
        return videoMeta.getPoster();
    }


    private ExecuteBinaryResponseHandler segHandler=new ExecuteBinaryResponseHandler(){
        @Override
        public void onFinish() {
            Log.d(TAG, "onFinish: finish segmenting " + videoMeta.getHash());
            videoAddChunk.finishSegment();
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
