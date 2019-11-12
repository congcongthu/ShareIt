package sjtu.opennet.honvideo;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;

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
}
