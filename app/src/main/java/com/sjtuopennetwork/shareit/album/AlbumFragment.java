package com.sjtuopennetwork.shareit.album;


import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.sjtuopennetwork.shareit.R;

import java.util.UUID;

import sjtu.opennet.textilepb.Model;
import sjtu.opennet.hon.Textile;

/**
 * A simple {@link Fragment} subclass.
 */
public class AlbumFragment extends Fragment {

    //UI控件
    LinearLayout album_photo_layout;
    LinearLayout album_video_layout;
    LinearLayout album_file_layout;


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

    }

    private void initUI() {
        album_photo_layout=getActivity().findViewById(R.id.album_photo_layout);
        album_video_layout=getActivity().findViewById(R.id.album_video_layout);
        album_file_layout=getActivity().findViewById(R.id.album_file_layout);

        album_photo_layout.setOnClickListener(v -> {
            Intent it=new Intent(getActivity(),PhotoActivity.class);
            startActivity(it);
        });
        album_video_layout.setOnClickListener(v -> {
            Intent it=new Intent(getActivity(),VideoActivity.class);
            startActivity(it);
        });
        album_file_layout.setOnClickListener(v -> {
            Intent it=new Intent(getActivity(),FileActivity.class);
            startActivity(it);
        });
    }
    
    //创建新的thread
    private void addNewThreads(String threadName){
        String key= UUID.randomUUID().toString();//随机生成key
        sjtu.opennet.textilepb.View.AddThreadConfig.Schema schema= sjtu.opennet.textilepb.View.AddThreadConfig.Schema.newBuilder()
                .setPreset(sjtu.opennet.textilepb.View.AddThreadConfig.Schema.Preset.MEDIA)
                .build();

        sjtu.opennet.textilepb.View.AddThreadConfig config=sjtu.opennet.textilepb.View.AddThreadConfig.newBuilder()
                .setSharing(Model.Thread.Sharing.NOT_SHARED)
                .setType(Model.Thread.Type.OPEN)
                .setKey(key).setName(threadName)//设置key和thread名字
                .setSchema(schema)
                .build();
        try {
            Textile.instance().threads.add(config);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
