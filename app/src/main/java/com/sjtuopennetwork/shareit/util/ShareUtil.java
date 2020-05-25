package com.sjtuopennetwork.shareit.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.sjtuopennetwork.shareit.R;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.UUID;

import sjtu.opennet.hon.Handlers;
import sjtu.opennet.hon.Textile;
import sjtu.opennet.textilepb.Model;
import sjtu.opennet.textilepb.View;

public class ShareUtil {
    private static final String TAG = "========================";
    private static String dir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/txtlimg/";
    private static String fileCacheDir=Environment.getExternalStorageDirectory().getAbsolutePath() + "/txtlimgcache/";
    private static String fileDir=Environment.getExternalStorageDirectory().getAbsolutePath() + "/txtlfile/";

    public static String storeSyncFile(byte[] data,String fileName){
        return saveFile(data,fileDir,fileName);
    }

    public static String isFileExist(String fileName){
        File judge=new File(fileDir+fileName);
        if(judge.exists()){
            return judge.getAbsolutePath();
        }
        return null;
    }

    private static String saveFile(byte[] data, String fileDir, String fileName){
        //创建文件夹
        File f = new File(fileDir);
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
            File file=new File(fileDir+fileName);
            Log.d(TAG, "saveFile: "+file.getAbsolutePath());
            if(!file.exists()){
                file.createNewFile();
            }
            FileOutputStream out=new FileOutputStream(file);
            out.write(data);
            finalNameWithDir=fileDir+fileName;
            out.close();
            return finalNameWithDir;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return finalNameWithDir;
    }

    public static String getHuaweiAvatar(String url){
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

    public static void createDeviceThread() {
        //看有没有"mydevice1219"，没有就新建
        try {
            if(getThreadByName("mydevice1219")==null){
                createThreadByName("mydevice1219");
            }
            if(getThreadByName("photo1219")==null){
                createThreadByName("photo1219");
            }
            if(getThreadByName("video1219")==null){
                createThreadByName("video1219");
            }
            if(getThreadByName("file1219")==null){
                createThreadByName("file1219");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void createThreadByName(String threadName){
        String key= UUID.randomUUID().toString();
        View.AddThreadConfig.Schema schema= View.AddThreadConfig.Schema.newBuilder()
                .setPreset(View.AddThreadConfig.Schema.Preset.BLOB)
                .build();
        View.AddThreadConfig config= View.AddThreadConfig.newBuilder()
                .setSharing(Model.Thread.Sharing.NOT_SHARED)
                .setType(Model.Thread.Type.PRIVATE)
                .setKey(key)
                .setName(threadName)
                .setSchema(schema)
                .build();
        try {
            Textile.instance().threads.add(config);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Model.Thread getThreadByName(String threadName){
        try {
            List<Model.Thread> threadList = Textile.instance().threads.list().getItemsList();
            for (Model.Thread t : threadList) {
                if(t.getName().equals(threadName)){
                    return t;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
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
            e.printStackTrace();
        }
    }

    public static String getMyAvatar(){
        try {
           return Textile.instance().profile.avatar();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
    public static String getOtherAvatar(String addr){
        try {
            return Textile.instance().contacts.get(addr).getAvatar();
        } catch (Exception e) {
//            e.printStackTrace();
        }
        return "";
    }
    public static String getMyName(){
        try {
            return Textile.instance().profile.name();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
    public static String getOtherName(String addr){
        try {
            return Textile.instance().contacts.get(addr).getName();
        } catch (Exception e) {
//            e.printStackTrace();
            return "null";
        }
    }

    private static boolean isImgInCache(String hash){
        String fileName=fileCacheDir+hash;
        Log.d(TAG, "isImgInCache: "+hash);
        File file=new File(fileName);
        return file.exists();
    }

    public static String cacheImg(byte[] data,String hash){
        String imgipfs=Environment.getExternalStorageDirectory().getAbsolutePath() + "/txtlimgcache/ipfs";
        File ipfsDir=new File(imgipfs);
        if(!ipfsDir.exists()){
            ipfsDir.mkdir();
        }
        Log.d(TAG, "cacheImg: test ipfsDir:"+ipfsDir.getAbsolutePath());
        return saveFile(data,fileCacheDir,hash);
    }

    public static void setImageView(Context context,ImageView imageView,String hash,int type){ // 0 avatar, 1 textile picture, 2 ipfs picture
        String fileDir=Environment.getExternalStorageDirectory().getAbsolutePath() + "/txtlimgcache/";
        String fileName=fileDir+hash;
        if(hash.equals("")){ //如果为空就用默认
            Glide.with(context).load(R.drawable.ic_album).thumbnail(0.3f).into(imageView);
        }else{
            //如果缓存中已有，就直接从缓存加载,否则异步加载
            if(isImgInCache(hash)){
                Glide.with(context).load(fileName).thumbnail(0.3f).into(imageView);
            }else{
                Handler handler=new Handler(){
                    @Override
                    public void handleMessage(Message msg) {
                        Glide.with(context).load(fileName).thumbnail(0.3f).into(imageView);
                    }
                };
                if(type==0){
                    Textile.instance().ipfs.dataAtPath("/ipfs/" + hash + "/0/small/content", new Handlers.DataHandler() {
                        @Override
                        public void onComplete(byte[] data, String media) {
                            cacheImg(data,hash);
                            Message msg=handler.obtainMessage();
                            handler.sendMessage(msg);
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.d(TAG, "onError: get image error: "+hash);
                            e.printStackTrace();
                        }
                    });
                }else if (type==1){ //文件图片
                    Textile.instance().files.content(hash, new Handlers.DataHandler() {
                        @Override
                        public void onComplete(byte[] data, String media) {
                            cacheImg(data,hash);
                            Message msg=handler.obtainMessage();
                            handler.sendMessage(msg);
                        }
                        @Override
                        public void onError(Exception e) {
                            Log.d(TAG, "onError: get image error: "+hash);
                            e.printStackTrace();
                        }
                    });
                }else if(type==2){
                    Textile.instance().ipfs.dataAtPath(hash, new Handlers.DataHandler() {
                        @Override
                        public void onComplete(byte[] data, String media) {
                            Log.d(TAG, "onComplete: 缓存ipfs图片成功："+data.length);
                            cacheImg(data,hash);
                            Message msg=handler.obtainMessage();
                            handler.sendMessage(msg);
                        }

                        @Override
                        public void onError(Exception e) {
                        }
                    });
                }
            }
        }
    }

    public static String getFileNameWithSuffix(String pathandname) {
        int start = pathandname.lastIndexOf("/");
        if (start != -1) {
            return pathandname.substring(start + 1);
        } else {
            return null;
        }
    }
}
