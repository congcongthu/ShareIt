package com.sjtuopennetwork.shareit.share;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.luck.picture.lib.photoview.PhotoView;
import com.sjtuopennetwork.shareit.R;
import com.sjtuopennetwork.shareit.util.FileUtil;

public class ImageInfoActivity extends AppCompatActivity {

    //UI控件
    PhotoView photoView;

    //内存数据
    String imgpath;
    String imghash;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_info);


        Intent it=getIntent();
        imghash=it.getStringExtra("imghash");

        imgpath= FileUtil.getFilePath(imghash);

        photoView=findViewById(R.id.photo_view);

        photoView.setImageBitmap(BitmapFactory.decodeFile(imgpath));
    }
}
