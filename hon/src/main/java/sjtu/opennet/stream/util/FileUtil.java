package sjtu.opennet.stream.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
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

    /**
     * Delete a directory (or file) and its contents recurisively.
     * @param fileOrDir
     */
    public static void deleteRecursive(File fileOrDir){
        if (fileOrDir.isDirectory())
            for (File child : fileOrDir.listFiles())
                deleteRecursive(child);
        fileOrDir.delete();
    }

    /**
     * Delete the contents inside a directory.
     * Note that the directory itself would not be removed.
     * @param directory
     */
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

    public static void createNewFile(String filePath){
        try {
            File file = new File(filePath);
            file.createNewFile();
        }catch(IOException ie){
            Log.e(TAG, String.format("IOException occur when create new file %s.", filePath));
            ie.printStackTrace();
        }
    }

    public static boolean fileExists(String filePath){
        File file = new File(filePath);
        return file.exists();
    }

    public static File[] listDir(String dir){
        File fDir = new File(dir);
        return fDir.listFiles();
    }

    public static boolean searchLocalVideo(String videoDir, String videoId){
        File[] fileList = listDir(videoDir);
        for(File file: fileList){
            if(file.isDirectory() && (videoId == file.getName())){
                return true;
            }
        }
        return false;
    }

    public static byte[] readAllBytes(String filePath){
        try (InputStream input = new FileInputStream(new File(filePath))){
            byte[] byt = new byte[input.available()];
            input.read(byt);
            return byt;
        }catch(FileNotFoundException fe){
            Log.e(TAG, String.format("File %s not found.", filePath));
            fe.printStackTrace();
            return null;
        }catch(IOException ie){
            Log.e(TAG, "Unexpected ioexception");
            ie.printStackTrace();
            return null;
        }
    }

    public static String readAllString(String filePath){
        return new String(readAllBytes(filePath));
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
}