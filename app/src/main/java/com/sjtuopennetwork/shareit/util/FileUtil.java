package com.sjtuopennetwork.shareit.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class FileUtil {
    private static final String TAG = "FileUtil";
    private static String dir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/txtlimg/";

    /**
     * 根据hash值获得文件完整路径名 v
     * @param hash
     * @return 如果文件已经存在则返回文件路径，如果不存在则返回null
     */
    public static String getFilePath(String hash){
        if(hash.equals("")){
            return "null";
        }
        File file=new File(dir+hash);
        String filePath="null";
        if(file.exists()){
            filePath=dir+hash;
            System.out.println("=========文件存在："+filePath);
        }else{
            System.out.println("=========文件不存在："+filePath);
        }
        return filePath;
    }

    /**
     * 将data数据存储为文件
     * @param data 得到的流数据
     * @param hash 文件hash值，直接作为文件名
     * @return 存储下来的包括路径的完整文件名
     */
    public static String storeFile(byte[] data,String hash){

        //创建文件夹
        File f = new File(dir);
        if(!f.exists()){
            f.mkdirs();
        }

        //获取存储状态，如果状态不是mounted，则无法读写，返回“null”
        String state = Environment.getExternalStorageState();
        if (!state.equals(Environment.MEDIA_MOUNTED)) {
            return "null";
        }

        String finalNameWithDir="null"; //最终的完整文件路径
        try {
            File file=new File(dir+hash);
            if(!file.exists()){
                file.createNewFile();
            }
            FileOutputStream out=new FileOutputStream(file);
            Bitmap bitmap= BitmapFactory.decodeByteArray(data,0,data.length);
            bitmap.compress(Bitmap.CompressFormat.JPEG,100,out);
            finalNameWithDir=dir+hash;
            out.close();
            return finalNameWithDir;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return finalNameWithDir;
    }


    public static String getHuaweiAvatar(String url){
        System.out.println("=============华为头像："+url);
        //创建文件夹
        File f = new File(dir);
        if(!f.exists()){
            f.mkdirs();
        }
        //获取存储状态，如果状态不是mounted，则无法读写，返回“null”
        String state = Environment.getExternalStorageState();
        if (!state.equals(Environment.MEDIA_MOUNTED)) {
            return "null";
        }

        String[] urls=url.split("/");

        String finalNameWithDir="null";
        try {
            URL aurl=new URL(url);
            URLConnection connection=aurl.openConnection();
            connection.setConnectTimeout(3000);
            InputStream inputStream=connection.getInputStream();
            byte[] bs=new byte[1024];
            File huaweiAvatar=new File(dir+urls[urls.length-1]);
            OutputStream outputStream=new FileOutputStream(huaweiAvatar);
            int len;
            while((len=inputStream.read(bs))!=-1){
                outputStream.write(bs,0,len);
            }
            outputStream.close();
            inputStream.close();
            finalNameWithDir=dir+urls[urls.length-1];
        } catch (Exception e) {
            e.printStackTrace();
        }
        return finalNameWithDir;
    }

    /**
     * Get Application's external storage file path.
     * @param context Activity context.
     * @param dirName If given, this method will try to find the specific sub directory in storage.
     *                And create one if it does not exist.
     * @return The absolute path of required directory.
     */
    public static String getAppExternalPath(Context context, String dirName){
        Log.i(TAG, String.format("Get path of %s dir in app external file directory.", dirName));
        String directoryPath = "";
        if(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())){
            directoryPath = context.getExternalFilesDir(dirName).getAbsolutePath();
        }else{
            Log.w(TAG, "No external storage available, try to use internal storage. (Time to buy a new phone :)");
            directoryPath = context.getFilesDir() + File.separator + dirName;
        }
        File file = new File(directoryPath);
        if(!file.exists()){
            Log.i(TAG, "Directory does not exists. Try to create one.");
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

    public static void saveBitmap(String filename,Bitmap bm){
        File f = new File(filename);
        if (f.exists()) {
            f.delete();
        }
        try {
            FileOutputStream out = new FileOutputStream(f);
            bm.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static File getM3u8FIle(String dir){
        String m3u8=dir+"/chunks/playlist.m3u8";
        File m3u8file=new File(m3u8);
        try{
            if(!m3u8file.exists()){ //从dir中找到文件复制到chunks里面
                m3u8file.createNewFile();
            }
            //复制
            File source=new File(dir+"/playlist.m3u8");
            FileUtils.copyFile(source,m3u8file);
        }catch (Exception e){
            e.printStackTrace();
        }
        return m3u8file;
    }

}
