package com.sjtuopennetwork.shareit.util;

import android.util.Log;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class LogToFTP {

    private static final String TAG = "==================";

    static FTPClient client =new FTPClient();

    private static final String host="111.231.247.56";
    private static final String uname="anonymous";
    private static final String password="";

    public static boolean uploadLogToFTP(String file){

        new Thread(){
            @Override
            public void run() {
                //建立连接
                try {
                    Log.d(TAG, "uploadLogToFTP: 开始连接");
                    client.connect(host);
                    Log.d(TAG, "uploadLogToFTP: 连接结果："+client.getReply());
                    if(FTPReply.isPositiveCompletion(client.getReplyCode())){
                        client.login(uname,password);
                    }

                    FileInputStream inputStream=new FileInputStream(file);
                    boolean uploadReply=client.storeFile("logs",inputStream);
                    Log.d(TAG, "uploadLogToFTP: 上传结果："+uploadReply);
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();

        return true;
    }

}
