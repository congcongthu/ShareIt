package sjtu.opennet.honvideo;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.util.Log;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;

import java.io.File;

public class Segmenter {
    public static final String TAG = "HONVIDEO.Segmenter";
    public static boolean isInit = false;
    private static int progressCounter = 0;
    public static ExecuteBinaryResponseHandler defaultHandler = new ExecuteBinaryResponseHandler(){
        @Override
        public void onSuccess(String message) {
            Log.i(TAG, String.format("Command success\n%s", message));
        }

        @Override
        public void onProgress(String message){
            progressCounter += 1;
            Log.i(TAG, String.format("cmd \t%d:\t%s", progressCounter, message));
        }

        @Override
        public void onFailure(String message) {
            progressCounter = 0;
            Log.e(TAG, "Command failure.");
        }

        @Override
        public void onStart() {
            progressCounter = 0;
        }

        @Override
        public void onFinish() {
            progressCounter = 0;
        }

    };

    public static void initFfmpeg(Context context) throws  FFmpegNotSupportedException{

        FFmpeg ffmpeg = FFmpeg.getInstance(context);
        ffmpeg.loadBinary(new LoadBinaryResponseHandler(){
            @Override
            public void onSuccess(){
                Log.i(TAG, "FFmpeg binary load Successfully.");
            }
            @Override
            public void onFailure(){
                Log.e(TAG, "FFmpeg binary load failure.");
            }
        });
        isInit = true;
    }

    /**
     * Get the chunk index from chunk file name.
     * This will be remove when we update our video chunk proto object definition.
     * @param path chunk file name
     * @return chunk index
     */
    public static int getIndFromPath(String path){
        return Integer.parseInt(path.substring(3,7));
    }

    public static void segment(Context context, int segTime, String filePath, String m3u8Path, String outDir, ExecuteBinaryResponseHandler handler) throws Exception{
        Log.i(TAG, String.format("Segment file %s into %s.", filePath, outDir));
        if(!isInit){
            Log.w(TAG, "FFmpeg has not be loaded. Try to load it.");
            initFfmpeg(context);
        }
        File outDirf = new File(outDir);
        if(!outDirf.exists()){
            outDirf.mkdir();
        }
        //String command = String.format("-i %s -c copy -bsf:v h264_mp4toannexb -map 0 -f segment -segment_time 10 -segment_list %s/out.m3u8 %s/out%%04d.ts", filePath, outDir, outDir);
//        String command = String.format("-i %s -c copy -bsf:v h264_mp4toannexb -map 0 -f segment " +
//                "-segment_time %d " +
//                "-segment_list %s " +
//                "%s/out%%04d.ts", filePath, segTime, m3u8Path, outDir);
        String command = String.format("-i %s -c copy -bsf:v h264_mp4toannexb -map 0 -f segment " +
                  "-segment_time %d " +
                  "-segment_list %s " +
                  "%s/out%%04d.ts", filePath, segTime, m3u8Path, outDir);
        FFmpeg ffmpeg = FFmpeg.getInstance(context);
        if(handler!=null){
            ffmpeg.execute(command.split(" "), handler);
        }else{
            ffmpeg.execute(command.split(" "), defaultHandler);
        }
    }
}
