package com.sjtuopennetwork.shareit.album;

import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.luck.picture.lib.PictureSelector;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.entity.LocalMedia;
import com.sjtuopennetwork.shareit.R;
import com.sjtuopennetwork.shareit.util.FileUtil;

import java.util.List;

import io.textile.pb.Model;
import io.textile.textile.Handlers;
import io.textile.textile.Textile;

import static android.app.PendingIntent.getActivity;

//读取photo_thread中的hash，将其转换为图片，并显示；
//添加添加本地图片按键，将本地图片同步到photo_thread
//同步功能
public class PhotoActivity extends AppCompatActivity {

    //UI控件
    ImageView photo_add;
    ImageView photo_sync;

    //
    private  String threadId;
    protected String[] mDataset;

    List<LocalMedia> choosePic;
    /**
     * 数据：myDataset
     * 主体：recycleview
     * 适配器：mAdapter
     */
    private RecyclerView recyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);

        initUI();
        //初始化
        //initData(threadId);
        initDataset();

        //初始化主体
        recyclerView = (RecyclerView) findViewById(R.id.my_recyclerview);
        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        recyclerView.setHasFixedSize(true);

        //配置layout manager
        GridLayoutManager layoutManager = new GridLayoutManager(this,3);
        recyclerView.setLayoutManager(layoutManager);

        // specify an adapter (see also next example)
        //初始化适配器
        mAdapter = new MyAdapter(mDataset);
        //配置adapter
        recyclerView.setAdapter(mAdapter);

    }

    public  void initUI()
    {
        photo_add = findViewById(R.id.photo_add);
        photo_sync=findViewById(R.id.photo_sync);
    }

    //初始化photo_thread
    //处理thread中的数据，将其显示
    //Get raw data for a file hash

    private void initData(String threadId) {

        String large_hash = null;
        try {
            large_hash = Textile.instance().files.list(threadId, "", 16).getItems(0).getFiles(0).getLinksMap().get("large").getHash();
        } catch (Exception e) {
            e.printStackTrace();
        }
        String finalLarge_hash = large_hash;
        Textile.instance().files.content(large_hash, new Handlers.DataHandler() {
            @Override
            public void onComplete(byte[] data, String media) {
                //完成后将文件路径存储到mappings表，并构造新的对象
                String filename = FileUtil.storeFile(data, finalLarge_hash);

            }
            @Override
            public void onError(Exception e) {

            }
        });

        //添加本地照片到photo thread
        photo_add.setOnClickListener(view -> {
            System.out.println("添加本地照片");
            PictureSelector.create(PhotoActivity.this)
                    .openGallery(PictureMimeType.ofImage())
                    .maxSelectNum(1)
                    .compress(false)
                    .forResult(PictureConfig.CHOOSE_REQUEST);
        });
        //检查thread上的照片，同步到相册
        photo_sync.setOnClickListener(view -> {

        });

    }
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==PictureConfig.CHOOSE_REQUEST && resultCode==RESULT_OK){
            choosePic=PictureSelector.obtainMultipleResult(data);
            String filePath=choosePic.get(0).getPath();

            //发送照片
//            Textile.instance().files.addFiles(filePath, threadid, "", new Handlers.BlockHandler() {
//                @Override
//                public void onComplete(Model.Block block) {
//                }
//                @Override
//                public void onError(Exception e) {
//                    e.printStackTrace();
//                }
//            });
        }
    }

    //添加图片到相册thread
    //参数为threadid和图片路径
    private  void  addPhotosToThread(String threadId,String filePath){
        //final String threadId = "<thread-id>";
        //String filePath = "/path/to/image";

        Textile.instance().files.addFiles(filePath, threadId, "", new Handlers.BlockHandler() {
            @Override
            public void onComplete(Model.Block block) {

            }

            @Override
            public void onError(Exception e) {

            }
        });

    }
    private void initDataset() {
        // DATASET_COUNT=60；
        mDataset = new String[60];
        for (int i = 0; i < 60; i++) {
            mDataset[i] = "Element #" + i;
        }
    }

}
