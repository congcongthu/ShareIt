package com.sjtuopennetwork.shareit.album;

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.leon.lfilepickerlibrary.LFilePicker;
import com.leon.lfilepickerlibrary.utils.Constant;
import com.luck.picture.lib.PictureSelector;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.entity.LocalMedia;
import com.sjtuopennetwork.shareit.R;
import com.sjtuopennetwork.shareit.util.AppdbHelper;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import sjtu.opennet.hon.Handlers;
import sjtu.opennet.hon.Textile;
import sjtu.opennet.textilepb.Model;

import static android.support.v7.widget.RecyclerView.VERTICAL;

public class FileActivity extends AppCompatActivity {

    public static int REQUESTCODE_FROM_ACTIVITY = 1000;
    private static final String TAG =  "FileActivity";
    //控件
    ImageView file_add;
    ImageView file_sync;
    RecyclerView recyclerView;
    private RecyclerView.Adapter fileAdapter;
    //持久化数据
    public SQLiteDatabase appdb;
    //private AppdbHelper fileDbHelper;
    SharedPreferences pref;
    private String thread_file_id="";


    String chooseFileName;
    String chooseFileTime;
    List<String> chooseFilePath;
    //往adapter中传输的数据
    String fileimage;
    List<String> fileName=new ArrayList<String>();
    List<String> fileTime=new ArrayList<String>();
    List<String> filePath=new ArrayList<String>();
    private String pathtest="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Set EventBus for Query Result.
        if(!EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().register(this);
        }

        setContentView(R.layout.activity_file);
        pref =getSharedPreferences("txt1",MODE_PRIVATE);
        thread_file_id = pref.getString("thread_file_id","");
        appdb=AppdbHelper.getInstance(this,"files_db").getWritableDatabase();



        initUI();
        initData(thread_file_id);
        initRecycleview();

        file_add.setOnClickListener(view->{
            new LFilePicker()
                    .withActivity(FileActivity.this)
                    .withRequestCode(REQUESTCODE_FROM_ACTIVITY)
                    .withMutilyMode(false)//false为单选
                    .withFileFilter(new String[]{".txt",".png",".jpeg",".jpg" })//设置可选文件类型
                    .withTitle("文件选择")//标题
                    .start();
        });
        file_sync.setOnClickListener(view->{
            initData(thread_file_id);
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

    private void initRecycleview() {
        //recyclerView.setHasFixedSize(true);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);
        //  recyclerView.addItemDecoration(new DividerItemDecoration(this,DividerItemDecoration.VERTICAL));//设置分割线
        fileAdapter = new FileAdapter(this,fileimage,fileName,fileTime,filePath);
        recyclerView.setAdapter(fileAdapter);
        fileAdapter.notifyDataSetChanged();
    }

    private void initData(String threadid) {
        //
        fileName.clear();
        fileTime.clear();
        filePath.clear();
        Cursor cursor_name=appdb.query("files",new String[]{"filename"},null,null,null,null,null);
        while(cursor_name.moveToNext()){
            String name = cursor_name.getString(cursor_name.getColumnIndex("filename"));
            fileName.add(name);
        }
        cursor_name.close();
        Cursor cursor_time=appdb.query("files",new String[]{"filetime"},null,null,null,null,null);
        while(cursor_time.moveToNext()){
            String time = cursor_time.getString(cursor_time.getColumnIndex("filetime"));
            fileTime.add(time);
        }
        cursor_time.close();
        Cursor cursor_path=appdb.query("files",new String[]{"filepath"},null,null,null,null,null);
        while(cursor_path.moveToNext()){
            String path = cursor_path.getString(cursor_path.getColumnIndex("filepath"));
            filePath.add(path);
        }
        cursor_path.close();
    }

    private void initUI() {
        file_add = findViewById(R.id.file_add);
        file_sync=findViewById(R.id.file_sync);
        recyclerView=findViewById(R.id.file_recyclerview);

    }
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUESTCODE_FROM_ACTIVITY&&resultCode == RESULT_OK) {
            //获取被选择图片的存储路径
            // chooseFileName =data.
            //chooseFilePath = data.getStringExtra("paths");
            //选择多个文件，使用getStringArrayListExtra,更改chooseFileName的类型

            chooseFilePath = data.getStringArrayListExtra("paths");
            for(int i=0;i<chooseFilePath.size();i++) {
                String path = chooseFilePath.get(i);
                chooseFileName=getFileNameWithSuffix(path);
                chooseFileTime=getSystemTime();
                ContentValues cv = new ContentValues();
                cv.put("filepath", path);
                cv.put("filename",chooseFileName);
                cv.put("filetime",chooseFileTime);
                appdb.insert("files", null, cv);
                initData(thread_file_id);
                fileAdapter.notifyDataSetChanged();


                //将获取到的文件存储在file thread
                pref = getSharedPreferences("txt1", MODE_PRIVATE);
                thread_file_id = pref.getString("thread_file_id", "");
                Textile.instance().files.addFiles(path, thread_file_id, "", new Handlers.BlockHandler() {
                    @Override
                    public void onComplete(Model.Block block) {
                        // handler.sendEmptyMessage(0);
                    }

                    @Override
                    public void onError(Exception e) {
                        e.printStackTrace();
                    }
                });

            }
        }



    }

    //无后缀
    private String getFileName(String pathandname){

        int start=pathandname.lastIndexOf("/");
        int end=pathandname.lastIndexOf(".");
        if(start!=-1 && end!=-1){
            return pathandname.substring(start+1,end);
        }else{
            return null;
        }
    }
    //有后缀
    private String getFileNameWithSuffix(String pathandname) {
        int start = pathandname.lastIndexOf("/");
        if (start != -1) {
            return pathandname.substring(start + 1);
        } else {
            return null;
        }
    }

    private String getSystemTime(){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss");// HH:mm:ss
        Date date = new Date(System.currentTimeMillis());
        String times = simpleDateFormat.format(date);
        return times;
    }


    @Subscribe(threadMode = ThreadMode.POSTING)
    public void getAnResult(Model.SyncFile sFile) {
        //addressMap.put(videoChunk.getChunk(),videoChunk.getAddress()); //拿到一个结果就放进来一个，可能会相同
        //videorHelper.receiveChunk(videoChunk); //将对应的视频保存到本地
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "Activity end. Unregister the eventbus.");
        if(EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().unregister(this);
        }
    }
}
