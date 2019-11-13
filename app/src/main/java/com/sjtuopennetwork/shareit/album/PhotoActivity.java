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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import sjtu.opennet.textilepb.Model;
import sjtu.opennet.hon.Handlers;
import sjtu.opennet.hon.Textile;
import sjtu.opennet.textilepb.View;

import static android.app.PendingIntent.getActivity;


public class PhotoActivity extends AppCompatActivity {

    //UI控件
    ImageView photo_add;
    ImageView photo_sync;
    //
    private  String thread_photo_id = "";
    List<String> myDataset=new ArrayList<String>();
    List<String> largeHash=new ArrayList<String>();
    protected String picDataset;
    List<LocalMedia> choosePic;

    //持久化数据
    SharedPreferences pref;

    private Lock lock = new ReentrantLock();

    /**
     * 数据：myDataset
     * 主体：recycleview
     * 适配器：mAdapter
     */
    private RecyclerView recyclerView;
    private RecyclerView.Adapter mAdapter;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);

        pref =getSharedPreferences("txt1",MODE_PRIVATE);
        thread_photo_id = pref.getString("thread_photo_id","");

        //同步所有thread中的照片到手机
        initData(thread_photo_id);

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
        //检查thread上的照片，同步到本地app存储
        photo_sync.setOnClickListener(view -> {
            // photoSync(thread_photo_id);

        });
    }
    private  void initUI()
    {
        photo_add = findViewById(R.id.photo_add);
        photo_sync=findViewById(R.id.photo_sync);
        recyclerView =  findViewById(R.id.my_recyclerview);
    }

    private  void  initRecycleView(){
        // mAdapter.notifyDataSetChanged();
        //recycleview配置
        //初始化主体
        recyclerView.setHasFixedSize(true);
        GridLayoutManager layoutManager = new GridLayoutManager(this,3);
        recyclerView.setLayoutManager(layoutManager);
        // mAdapter=new MyAdapter();
        //recyclerView.setAdapter(mAdapter);
        mAdapter = new MyAdapter(myDataset);
        recyclerView.setAdapter(mAdapter);
    }

    //得到photo thread中的所有hash
    //将hash转为本地路径
    //设置适配器
    private  void initData(String threadId){
        try {
            lock.lock();
            myDataset.clear();
            largeHash.clear();
            int listnum = 0;
            try {
                listnum = Textile.instance().files.list(threadId, "", 256).getItemsCount();
            } catch (Exception e) {
                e.printStackTrace();
            }

            // System.out.println("===============Item个数："+listnum);
            //得到photo thread中所有hash
            for (int i = 0; i < listnum; i++) {
                String large_hash = "";
                try {

                    large_hash = Textile.instance().files.list(threadId, "", listnum).getItems(i).getFiles(0).getLinksMap().get("large").getHash();
                    //  choosePic=Textile.instance().files.list(threadId,"",listnum);
                    System.out.println("===================================photo_thread hash" + i + ":   " + large_hash);

                } catch (Exception e) {
                    e.printStackTrace();
                }
                //排除相同hash，即相同的图片
                //    if(!largeHash.contains(large_hash)){
                largeHash.add(large_hash);
                //   }

            }
            for (int i = 0; i < largeHash.size(); i++) {
                int finalI = i;
                String filepath = FileUtil.getFilePath(largeHash.get(finalI));
                if (filepath.equals("null")) {
                    Textile.instance().files.content(largeHash.get(i), new Handlers.DataHandler() {
                        @Override
                        public void onComplete(byte[] data, String media) {
                            //存储下来的包括路径的完整文件名
                            picDataset = FileUtil.storeFile(data, largeHash.get(finalI));
                            System.out.println("=========================文件不存在取得 " + picDataset);
                            myDataset.add(picDataset);
                        }
                        @Override
                        public void onError(Exception e) {
                        }
                    });
                } else {
                    myDataset.add(filepath);
                }
            }
            //
            for (int i = 0; i < myDataset.size(); i++) {
                if (myDataset.get(i).equals(null)) {
                    myDataset.remove(i);
                }
            }
            Collections.reverse(myDataset);
        } catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            lock.unlock();
        }


    }

    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PictureConfig.CHOOSE_REQUEST && resultCode == RESULT_OK) {
            //myDataset.clear();
            //选择或拍摄照片之后的回调，将对应图片添加到photo_thread中
            choosePic = PictureSelector.obtainMultipleResult(data);
            String filePath = choosePic.get(0).getPath();
            pref = getSharedPreferences("txt1", MODE_PRIVATE);
            thread_photo_id = pref.getString("thread_photo_id", "");
            Textile.instance().files.addFiles(filePath, thread_photo_id, "", new Handlers.BlockHandler() {//调用textile接口添加图片到photo_thread
                @Override
                public void onComplete(Model.Block block) {
                }

                @Override
                public void onError(Exception e) {
                    e.printStackTrace();
                }
            });
            try {
                Textile.instance().threads.snapshot();
            } catch (Exception e) {
                e.printStackTrace();
            }
            myDataset.add(filePath);
            mAdapter.notifyDataSetChanged();
        }
    }


}
