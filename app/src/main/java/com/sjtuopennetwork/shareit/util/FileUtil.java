package com.sjtuopennetwork.shareit.util;

import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;

public class FileUtil {

    private static String dir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/txtlimg/";

    /**
     * 根据hash值获得文件完整路径名
     * @param hash
     * @return 如果文件已经存在则返回文件路径，如果不存在则返回null
     */
    public static String getFilePath(String hash){
        File file=new File(dir+hash);
        String filePath="null";
        if(file.exists()){
            filePath=dir+hash;
            System.out.println("=========文件存在"+filePath);
        }else{
            System.out.println("=========文件不存在");
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
}
