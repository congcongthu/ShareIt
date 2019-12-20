package com.sjtuopennetwork.shareit.util;

import android.util.Log;

import java.io.File;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import sjtu.opennet.hon.Textile;

public class LogToHTTP {

    private static final String TAG = "============";

    private static String url="http://159.138.58.61:9999/uploadfile/";

    public static void uploadLog(String filename)  {

        OkHttpClient client=new OkHttpClient();

        File logFile=new File(filename);

        Log.d(TAG, "uploadLog: 上传文件的路径："+logFile.getAbsolutePath());

        String peerID= "";
        try {
            peerID=Textile.instance().profile.get().getId();
        } catch (Exception e) {
            e.printStackTrace();
        }

        RequestBody requestBody=new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("myfile",peerID+".log",RequestBody.create(MediaType.parse("multipart/form-data"), logFile))
                .build();

        Request request=new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        new Thread(){
            @Override
            public void run() {
                try {
                    Response response=client.newCall(request).execute();
                    Log.d(TAG, "run: 上传结果："+response.body().string());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();

        Log.d(TAG, "uploadLog: 上传结束");

    }

}
