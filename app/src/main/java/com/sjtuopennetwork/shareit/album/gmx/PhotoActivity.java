package com.sjtuopennetwork.shareit.album;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.luck.picture.lib.PictureSelector;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.entity.LocalMedia;
import com.sjtuopennetwork.shareit.R;
import com.sjtuopennetwork.shareit.util.AppdbHelper;
import com.sjtuopennetwork.shareit.util.FileUtil;
import com.sjtuopennetwork.shareit.util.SyncFileUtil;
import com.syd.oden.circleprogressdialog.core.CircleProgressDialog;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import sjtu.opennet.textilepb.Model;
import sjtu.opennet.hon.Handlers;
import sjtu.opennet.hon.Textile;
import sjtu.opennet.textilepb.View;

import static android.app.PendingIntent.getActivity;


public class PhotoActivity extends AppCompatActivity {
    //阻塞队列
    private BlockingQueue<Integer> bQueue =  new LinkedBlockingQueue<>();
    //持久化
    public SQLiteDatabase appdb;
    //UI控件
    ImageView photo_add;
    ImageView photo_sync;
    //
    List<String> photoPath=new ArrayList<String>();
    List<LocalMedia> choosePic;
    private SyncFileUtil mSyncFile;
    SharedPreferences pref;

    //持久化数据
    private String peerid;

    private Lock lock = new ReentrantLock();

    //recycleview
    private RecyclerView recyclerView;
    private RecyclerView.Adapter mAdapter;

    //


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Set EventBus for Query Result.
        if(!EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().register(this);
        }

        setContentView(R.layout.activity_photo);
        //pref =getSharedPreferences("txt1",MODE_PRIVATE);
        //thread_photo_id = pref.getString("thread_photo_id","");


        pref=getSharedPreferences("txtl",MODE_PRIVATE);
        appdb= AppdbHelper.getInstance(getApplicationContext(),pref.getString("loginAccount","")).getWritableDatabase();

        try {
            peerid=Textile.instance().account.address();
        } catch (Exception e) {
            e.printStackTrace();
        }


        //同步所有thread中的照片到手机
        initDataset();
        initUI();
        initRecycleView();

        //添加本地照片到photo thread
        photo_add.setOnClickListener(view -> {
            System.out.println("============================添加本地照片");
            PictureSelector.create(PhotoActivity.this)
                    .openGallery(PictureMimeType.ofImage())//全部.PictureMimeType.ofAll()、图片.ofImage()、视频.ofVideo()
                    .maxSelectNum(1)//最大图片选择数量
                    .compress(false)//是否压缩
                    .forResult(PictureConfig.CHOOSE_REQUEST);//结果回调 onActivityResult code
        });


        //检查cafe上的照片，同步到本地app存储
        photo_sync.setOnClickListener(view -> {
            SyncFileUtil.searchSyncFiles(peerid, Model.SyncFile.Type.PHOTO);
        });

    }
    @Override
    protected void onStart(){
        super.onStart();
        //Set EventBus for Query Result.
        if(!EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().register(this);
        }
    }

    private  void initUI() {
        photo_add = findViewById(R.id.photo_add);
        photo_sync=findViewById(R.id.photo_sync);
        recyclerView =  findViewById(R.id.my_recyclerview);
    }

    private  void  initRecycleView(){
        //recycleview配置
        //初始化主体

        recyclerView.setHasFixedSize(true);
        GridLayoutManager layoutManager = new GridLayoutManager(this,3);
        recyclerView.setLayoutManager(layoutManager);
        mAdapter = new PhotoAdapter(this,photoPath);
        recyclerView.setAdapter(mAdapter);
        mAdapter.notifyDataSetChanged();
    }

    //初始化数据集
    //得到数据库中的所有文件的路径
    private void initDataset(){
        System.out.println("-------------------------init path");
        photoPath.clear();
        Cursor cursor_path=appdb.query("photo",new String[]{"photopath"},null,null,null,null,null);
        while(cursor_path.moveToNext()){
            String path = cursor_path.getString(cursor_path.getColumnIndex("photopath"));
            photoPath.add(path);
            System.out.println("-------------------------init path"+path);
        }
        cursor_path.close();
    }


    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PictureConfig.CHOOSE_REQUEST && resultCode == RESULT_OK) {

            choosePic = PictureSelector.obtainMultipleResult(data);
            String filePath = choosePic.get(0).getPath();
            //添加图片到cafe节点
            mSyncFile = new SyncFileUtil(filePath,peerid, Model.SyncFile.Type.PHOTO);
            mSyncFile.Add();

            //添加图片路径到本地数据库，将data添加到本地文件夹内
            String name_photo=getFileNameWithSuffix(filePath);
            // byte[] data_photo=;
            String filePath_ =storePhoto(filePath,name_photo);
            ContentValues cv = new ContentValues();
            cv.put("photopath", filePath_);
            appdb.insert("photo", null, cv);
            System.out.println("--------------"+filePath_);

            photoPath.add(filePath_);
            mAdapter.notifyDataSetChanged();

            //  SyncFileUtil.searchSyncFiles(peerid, Model.SyncFile.Type.PHOTO);





        }
    }



    Handler handler_1 = new Handler() {
        @Override
        public void handleMessage(Message msg) {// handler接收到消息后就会执行此方法
            System.out.println("======================刷新页面");
            initDataset();
            mAdapter.notifyDataSetChanged();

        }
    };

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void getAnResult(Model.SyncFile sFile) {
        System.out.println("-----------------------photo getfile"+ sFile.getFile());
        String mHash = sFile.getFile();

//        String filepath = FileUtil.getFilePath(mHash);
//        if (filepath.equals("null")) {
//            Textile.instance().ipfs.dataAtPath(mHash, new Handlers.DataHandler() {
//                @Override
//                public void onComplete(byte[] data, String media) {
//                    picPath = FileUtil.storeFile(data, mHash);
//                    System.out.println("=========================文件不存在取得 " + picPath);
//                    dataset.add(picPath);
//
//                    handler_1.sendEmptyMessage(0);
//                }
//
//                @Override
//                public void onError(Exception e) {
//                }
//            });
//        }
//        else{
//            dataset.add(filepath);
//        }

    }
    private String getFileNameWithSuffix(String pathandname) {
        int start = pathandname.lastIndexOf("/");
        if (start != -1) {
            return pathandname.substring(start + 1);
        } else {
            return null;
        }
    }

    private String storePhoto(String oldPath,String name) {
        String dir = Environment.getExternalStorageDirectory().getAbsolutePath()+"/txtlphoto/";

        //创建文件夹
        File f = new File(dir);
        if (!f.exists()) {
            f.mkdirs();
        }

        //获取存储状态，如果状态不是mounted，则无法读写，返回“null”
        String state = Environment.getExternalStorageState();
        if (!state.equals(Environment.MEDIA_MOUNTED)) {
            return "null";
        }

        String finalNameWithDir = "null"; //最终的完整文件路径
        try {
            FileInputStream fileInputStream = new FileInputStream(oldPath);
            FileOutputStream fileOutputStream = new FileOutputStream(dir+name);
            byte[] buffer = new byte[1024];
            int byteRead;
            while (-1 != (byteRead = fileInputStream.read(buffer))) {
                fileOutputStream.write(buffer, 0, byteRead);
            }
            fileInputStream.close();
            fileOutputStream.flush();
            fileOutputStream.close();

            finalNameWithDir=dir+name;
            return finalNameWithDir;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return finalNameWithDir;
    }
    @Override
    public void onStop() {
        super.onStop();
        //  Log.d(TAG, "Activity end. Unregister the eventbus.");
        if(EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().unregister(this);
        }
    }

}
