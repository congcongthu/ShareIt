package com.sjtuopennetwork.shareit.album;


import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.sjtuopennetwork.shareit.R;

import java.util.List;
import java.util.UUID;

import sjtu.opennet.textilepb.Model;
import sjtu.opennet.hon.Textile;
import sjtu.opennet.textilepb.QueryOuterClass;

import static android.content.Context.MODE_PRIVATE;

/**
 * A simple {@link Fragment} subclass.
 */
public class AlbumFragment extends Fragment {

    //UI控件
    LinearLayout album_photo_layout;
    LinearLayout album_video_layout;
//    LinearLayout album_file_layout;

    //
    private String thread_photo_name="2019-11-3119:09:17.16929628-29692";
    private String thread_file_name= "2019-11-3119:09:17.16929628-29693";
    private String thread_video_name="2019-11-3119:09:17.16929628-29694";
    private  String thread_photo_id="";
    private  String thread_file_id="";
    private  String thread_video_id="";
    private boolean thread_photo_flag=false;
    private boolean thread_file_flag=false;
    private boolean thread_video_flag=false;


    public AlbumFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_album, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();

        initUI();

        List<Model.Thread> threads;
        try {
            threads = Textile.instance().threads.list().getItemsList();
            System.out.println("==========================本peer 的 thread 个数："+Textile.instance().threads.list().getItemsCount());
            System.out.println("=========================本peer 的 thread 个数： "+threads.size());
            for(Model.Thread t:threads){//遍历所有一个peer下的所有thread
                System.out.println("=================thread name:"+t.getName());
                if(t.getSharing().equals(Model.Thread.Sharing.NOT_SHARED)){
                    if(t.getName().equals(thread_photo_name)){
                        thread_photo_flag=true;
                        thread_photo_id=t.getId();
                        System.out.println("=======================photo thread已存在： "+thread_photo_id);
                    }
                    if(t.getName().equals(thread_file_name)){
                        thread_file_flag=true;
                        thread_file_id=t.getId();
                        System.out.println("=======================file thread已存在： "+thread_file_id);
                    }
                    if(t.getName().equals(thread_video_name)){
                        thread_video_flag=true;
                        thread_video_id=t.getId();
                        System.out.println("=======================video thread已存在： "+thread_video_id);
                    }
                }
            }

            if(!thread_photo_flag){
                thread_photo_id=addNewThreads(thread_photo_name);
                System.out.println("==============创建photo_thread:   "+thread_photo_id);
                thread_photo_flag=true;
            }
            if(!thread_file_flag){
                thread_file_id=addNewThreads(thread_file_name);
                System.out.println("==============创建file_thread:   "+thread_file_id);
                thread_file_flag=true;
            }
            if(!thread_video_flag){
                thread_video_id=addNewThreads(thread_video_name);
                System.out.println("==============创建video_thread:   "+thread_video_id);
                thread_video_flag=true;
            }
            SharedPreferences.Editor editor = getActivity().getSharedPreferences("txt1",MODE_PRIVATE).edit();
            editor.putString("thread_photo_id",thread_photo_id);
            editor.putString("thread_file_id",thread_file_id);
            editor.putString("thread_video_id",thread_video_id);
            editor.apply();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }



    private void initUI() {
        album_photo_layout=getActivity().findViewById(R.id.album_photo_layout);
        album_video_layout=getActivity().findViewById(R.id.album_video_layout);
//        album_file_layout=getActivity().findViewById(R.id.album_file_layout);

        album_photo_layout.setOnClickListener(v -> {
            Intent it=new Intent(getActivity(),PhotoActivity.class);
            startActivity(it);
        });
        album_video_layout.setOnClickListener(v -> {
            Intent it=new Intent(getActivity(),VideoActivity.class);
            startActivity(it);
        });
//        album_file_layout.setOnClickListener(v -> {
//            Intent it=new Intent(getActivity(),FileActivity.class);
//            startActivity(it);
//        });
    }

    //创建新的thread
    private String addNewThreads(String threadName){
        Model.Thread thread = null;
        String key= UUID.randomUUID().toString();//随机生成key
        sjtu.opennet.textilepb.View.AddThreadConfig.Schema schema= sjtu.opennet.textilepb.View.AddThreadConfig.Schema.newBuilder()
                .setPreset(sjtu.opennet.textilepb.View.AddThreadConfig.Schema.Preset.MEDIA)
                .build();

        sjtu.opennet.textilepb.View.AddThreadConfig config=sjtu.opennet.textilepb.View.AddThreadConfig.newBuilder()
                .setSharing(Model.Thread.Sharing.NOT_SHARED)
                .setType(Model.Thread.Type.PRIVATE)
                .setKey(key).setName(threadName)//设置key和thread名字
                .setSchema(schema)
                .build();
        try {
            thread = Textile.instance().threads.add(config);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //    Textile.instance().account.sync();

        return thread.getId();
    }

}
