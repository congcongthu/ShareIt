package com.sjtuopennetwork.shareit.share;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.luck.picture.lib.photoview.PhotoView;
import com.sjtuopennetwork.shareit.R;
import com.sjtuopennetwork.shareit.util.FileUtil;

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

//        if(imgpath.charAt(0)=='Q'){ //那就是hash值
//            imgpath= FileUtil.getFilePath(imgpath);
//        }

        photoView=findViewById(R.id.photo_view);
        if(imgpath.equals("null")){
            Toast.makeText(this,"图片获取失败",Toast.LENGTH_SHORT).show();
        }else{
            photoView.setImageBitmap(BitmapFactory.decodeFile(imgpath));
        }

        photoView.setOnClickListener(view -> finish());
    }
}
