package sjtu.opennet.hon;

import android.content.Context;
import android.util.Log;


//import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;


import org.bytedeco.ffmpeg.avformat.*;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.PointerPointer;

import static org.bytedeco.ffmpeg.global.avformat.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.util.Arrays;

//import com.writingminds.*;

public class Segmenter {
    private static final String TAG = "Segmenter";

    public static void initffmpeg(Context context){
        FFmpeg ffmpeg = FFmpeg.getInstance(context);
        try {
            ffmpeg.loadBinary(new LoadBinaryResponseHandler() {
                @Override
                public void onSuccess(){
                    Log.i(TAG, "FFmpeg binary load Successfully!!");
                }

                @Override
                public void onFailure() {
                    Log.i(TAG, "FFmpeg is not supported by your device.");
                }
            });
        } catch (FFmpegNotSupportedException e) {
            // Handle if FFmpeg is not supported by device
            Log.i(TAG, "FFmpeg is not supported by your device.");
        }

        try{
            ffmpeg.execute("-version".split(" "), new ExecuteBinaryResponseHandler(){
                @Override
                public void onSuccess(String message){
                    Log.i(TAG, String.format("Command Success\n%s", message));
                }

                @Override
                public void onFailure(String message){
                    Log.e(TAG, message);
                }
            });
        }catch(FFmpegCommandAlreadyRunningException e){
            Log.w(TAG, "FFmpeg is still running");
        }

    }

    public static void testExternalStorage(){
        Log.i(TAG, "Test the external storage permission");
        try{
            File f = new File("/storage/emulated/0/Download/test.txt");
            f.createNewFile();
            FileOutputStream fos = new FileOutputStream(f);
            PrintWriter pw = new PrintWriter(fos);
            pw.write("aaaaaaaaa");
            pw.flush();
            pw.close();
            fos.close();

            InputStream is = new FileInputStream(f);
            int iAvail = is.available();
            byte[] bytes = new byte[iAvail];
            is.read(bytes);
            Log.i("%s", "文件内容:\n" + new String(bytes));
            is.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    /*
    public static void segment(String filePath, String outDir){
        String filePathTest = "/storage/emulated/0/PictureSelector/CameraImage/PictureSelector_20191106_210140.mp4";
        Log.i(TAG, String.format("Segment file %s", filePathTest));

        String ffmpeg = Loader.load(org.bytedeco.ffmpeg.ffmpeg.class);
        ProcessBuilder pb = new ProcessBuilder(ffmpeg, "-i", filePathTest, "-vcodec", "h264", "/storage/emulated/0/Download/output.mp4");
        //ProcessBuilder pb = new ProcessBuilder(ffmpeg, "-version");
        try {
            pb.inheritIO().start().waitFor();
        }catch(IOException ie){
            ie.printStackTrace();
        }catch(InterruptedException te){
            te.printStackTrace();
        }
    }
    */



    public static void getFrame(String filePath){
        Log.i(TAG, String.format("Try to get frame from %s." , filePath));
        int ret;
        AVFormatContext fmt_ctx = new AVFormatContext(null);
        ret = avformat_open_input(fmt_ctx, filePath, null, null);
        avformat_close_input(fmt_ctx);

    }

    public static void testFFresume(Context context){
        Log.i(TAG, "Test ffmpeg resume");
        FFmpeg ffmpeg = FFmpeg.getInstance(context);
        try{
            ffmpeg.execute("-version".split(" "), new ExecuteBinaryResponseHandler(){
                @Override
                public void onSuccess(String message){
                    Log.i(TAG, String.format("Command Success\n%s", message));
                }

                @Override
                public void onFailure(String message){
                    Log.e(TAG, message);
                }
            });
        }catch(FFmpegCommandAlreadyRunningException e){
            Log.w(TAG, "FFmpeg is still running");
        }
    }

    public static void testSegment(Context context, String inputpath){
        Log.i(TAG, "Test ffmpeg segment");
        Log.i(TAG, String.format("Input file %s", inputpath));
        String outDir = "/storage/emulated/0/Download/OutDir";
        File outDirf = new File(outDir);
        if(!outDirf.exists()){
            Log.i(TAG, String.format("Test our directory does not exists. Try to create %s.", outDir));
            outDirf.mkdir();
        }
        //String command += "-i " + inputpath;
        String command = String.format("-i %s -map 0 -codec:v libx264 -codec:a aac -c:s dvdsub -f ssegment -segment_time 10 -segment_list %s/out.m3u8 %s/out%%03d.ts", inputpath, outDir, outDir);

        Log.i(TAG, String.format("Execute command:\n%s", command));

        FFmpeg ffmpeg = FFmpeg.getInstance(context);

        try{
            ffmpeg.execute(command.split(" "), new ExecuteBinaryResponseHandler(){
                @Override
                public void onSuccess(String message){
                    Log.i(TAG, String.format("Command Success\n%s", message));
                }

                @Override
                public void onFailure(String message){
                    Log.e(TAG, message);
                }
            });
        }catch(FFmpegCommandAlreadyRunningException e){
            Log.w(TAG, "FFmpeg is still running");
        }
    }
}
