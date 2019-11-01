package com.sjtuopennetwork.shareit.share;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.luck.picture.lib.photoview.PhotoView;
import com.sjtuopennetwork.shareit.R;

public class ImageInfoActivity extends AppCompatActivity {

    //UI控件
    PhotoView photoView;

    //内存数据
    String imgpath;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_info);


        Intent it=getIntent();
        imgpath=it.getStringExtra("imgpath");

        photoView=findViewById(R.id.photo_view);

        photoView.setImageBitmap(BitmapFactory.decodeFile(imgpath));

    }
}
