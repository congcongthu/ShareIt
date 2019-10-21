package com.sjtuopennetwork.shareit.util;

import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;

import java.io.File;

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
}
