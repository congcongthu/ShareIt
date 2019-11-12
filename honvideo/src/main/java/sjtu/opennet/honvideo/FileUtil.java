package sjtu.opennet.honvideo;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

public class FileUtil {
    private static final String TAG = "HONVIDEO.FileUtil";
    /**
     * Get Application's external storage file path.
     * @param context Activity context.
     * @param dirName If given, this method will try to find the specific sub directory in storage.
     *                And create one if it does not exist.
     * @return The absolute path of required directory.
     */
    public static String getAppExternalPath(Context context, String dirName){
        if(dirName == null){
            dirName = "";
        }
        String directoryPath = "";
        if(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())){
            directoryPath = context.getExternalFilesDir(dirName).getAbsolutePath();
        }else{
            Log.w(TAG, "No external storage available, try to use internal storage. (Time to buy a new phone :)");
            directoryPath = context.getFilesDir() + File.separator + dirName;
        }
        File file = new File(directoryPath);
        if(!file.exists()){
            Log.d(TAG, String.format("Directory %s does not exists. Try to create one.", directoryPath));
            file.mkdir();
        }
        return directoryPath;
    }

    public static void deleteRecursive(File fileOrDir){
        if (fileOrDir.isDirectory())
            for (File child : fileOrDir.listFiles())
                deleteRecursive(child);
        fileOrDir.delete();
    }

    public static void deleteContents(File directory){
        if (!directory.isDirectory()){
            Log.e(TAG, "%s is not a directory.\n" +
                    "This func is used to delete contents inside a directory.");
        }else{
            for (File child : directory.listFiles())
                deleteRecursive(child);
        }
    }

    public static void writeByteArrayToFile(String path, byte[] byteArray){
        try{
            File f = new File(path);
            OutputStream fout = new FileOutputStream(f);
            fout.write(byteArray);
            fout.flush();
            fout.close();
        }catch(FileNotFoundException fe){
            Log.e(TAG, String.format("File %s not found.", path));
            fe.printStackTrace();
        }catch(IOException ie){
            Log.e(TAG, "Unknown IOException");
            ie.printStackTrace();
        }
    }

    public static void appendToFileWithNewLine(String filePath, String text){
        try(FileWriter fw = new FileWriter(filePath, true);
            PrintWriter out = new PrintWriter(fw))
        {
            out.println(text);
        }catch(IOException ie){
            Log.e(TAG, String.format("IOException occur when write to %s.", filePath));
        }
    }
}
