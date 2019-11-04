package com.sjtuopennetwork.shareit.album;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.luck.picture.lib.PictureSelector;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.entity.LocalMedia;
import com.sjtuopennetwork.shareit.R;
import com.sjtuopennetwork.shareit.util.FileUtil;

import java.util.ArrayList;
import java.util.List;

import sjtu.opennet.textilepb.Model;
import sjtu.opennet.hon.Handlers;
import sjtu.opennet.hon.Textile;
import sjtu.opennet.textilepb.View;

import static android.app.PendingIntent.getActivity;

//读取photo_thread中的hash，将其转换为图片，并显示；
//添加添加本地图片按键，将本地图片同步到photo_thread
//同步功能
public class PhotoActivity extends AppCompatActivity {

    //UI控件
    ImageView photo_add;
    ImageView photo_sync;
    //
    private  String thread_photo_id = "";

    List<String> mDataset=new ArrayList<String>();
    List<String> largeHash=new ArrayList<String>();

    protected String picDataset;

    List<LocalMedia> choosePic;

    //持久化数据
    SharedPreferences pref;

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

        pref =getSharedPreferences("txt1",MODE_PRIVATE);
        thread_photo_id = pref.getString("thread_photo_id","");
        initData(thread_photo_id);

        //recycleview配置
        //初始化主体

        recyclerView = (RecyclerView) findViewById(R.id.my_recyclerview);
        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        recyclerView.setHasFixedSize(true);
        //配置layout manager
        GridLayoutManager layoutManager = new GridLayoutManager(this,3);
        recyclerView.setLayoutManager(layoutManager);
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

        //同步所有thread中的照片到手机
        photoSync(threadId);

        //添加本地照片到photo thread
        photo_add.setOnClickListener(view -> {
            System.out.println("============================添加本地照片");
            PictureSelector.create(PhotoActivity.this)
                    .openGallery(PictureMimeType.ofImage())//全部.PictureMimeType.ofAll()、图片.ofImage()、视频.ofVideo()
                    .maxSelectNum(1)//最大图片选择数量
                    .compress(false)//是否压缩
                    .forResult(PictureConfig.CHOOSE_REQUEST);//结果回调 onActivityResult code
        });
        //检查thread上的照片，同步到本地app存储
        photo_sync.setOnClickListener(view -> {
            photoSync(threadId);
        });

    }
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==PictureConfig.CHOOSE_REQUEST && resultCode==RESULT_OK){
            //选择或拍摄照片之后的回调，将对应图片添加到photo_thread中
            choosePic=PictureSelector.obtainMultipleResult(data);
            String filePath=choosePic.get(0).getPath();

            System.out.println("======================file path "+filePath);
            pref =getSharedPreferences("txt1",MODE_PRIVATE);
            thread_photo_id = pref.getString("thread_photo_id","");
            Textile.instance().files.addFiles(filePath, thread_photo_id, "", new Handlers.BlockHandler() {//调用textile接口添加图片到photo_thread
                @Override
                public void onComplete(Model.Block block) {
                }
                @Override
                public void onError(Exception e) {
                    e.printStackTrace();
                }
            });



        }

    }

    private void photoSync(String threadId){
        int listnum = 0;
        try {
            listnum = Textile.instance().files.list(threadId,"",256).getItemsCount();
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("===============Item个数："+listnum);

        for(int i=0;i<listnum;i++) {
            String large_hash = "";
            try {

                large_hash = Textile.instance().files.list(threadId, "", listnum).getItems(i).getFiles(0).getLinksMap().get("large").getHash();
                //  choosePic=Textile.instance().files.list(threadId,"",listnum);
                System.out.println("===================================photo_thread hash:   " + large_hash);

            } catch (Exception e) {
                e.printStackTrace();
            }

            //排除相同hash，即相同的图片
            if(!largeHash.contains(large_hash)){
                largeHash.add(large_hash);
            }

        }
        System.out.println("===================================hash个数：  " + largeHash.size());



        for(int i=0;i<largeHash.size();i++) {
            int finalI = i;
            String filepath = FileUtil.getFilePath(largeHash.get(finalI));
            if(filepath.equals("null")){
                Textile.instance().files.content(largeHash.get(i), new Handlers.DataHandler() {
                    @Override
                    public void onComplete(byte[] data, String media) {
                        //存储下来的包括路径的完整文件名
                        picDataset = FileUtil.storeFile(data, largeHash.get(finalI));
                        mDataset.add(picDataset);
                    }
                    @Override
                    public void onError(Exception e) {
                    }
                });
            }
            else{
                mDataset.add(filepath);
            }

        }
        //
        for(int i=0;i<mDataset.size();i++){
            if(mDataset.get(i).equals(null)){
                mDataset.remove(i);
            }
        }


//        mAdapter = new MyAdapter(mDataset);
//        recyclerView.setAdapter(mAdapter);

        //取出thread中的每个图片hash
//        String large_hash = "";
//        try {
//
//            large_hash = Textile.instance().files.list(threadId, "", listnum).getItems(2).getFiles(0).getLinksMap().get("large").getHash();
//          //  choosePic=Textile.instance().files.list(threadId,"",listnum);
//            System.out.println("===================================photo_thread hash:   "+large_hash);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        String finalLarge_hash = large_hash;
//
//        Textile.instance().files.content(large_hash, new Handlers.DataHandler() {
//            @Override
//            public void onComplete(byte[] data, String media) {
//                //存储下来的包括路径的完整文件名
//                picDataset = FileUtil.storeFile(data, finalLarge_hash);
//                System.out.println("===================================picture path:   "+picDataset);
//
//            }
//            @Override
//            public void onError(Exception e) {
//            }
//        });
    }





}
