package com.sjtuopennetwork.shareit.album;


import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.sjtuopennetwork.shareit.R;

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


}
