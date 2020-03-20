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
    byte[] photoData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_info);
        photoView=findViewById(R.id.photo_view);

        Intent it=getIntent();
        Bundle b=it.getExtras();


        photoData=b.getByteArray("photoData");
        if(photoData!=null){
            photoView.setImageBitmap(BitmapFactory.decodeByteArray(photoData,0,photoData.length));
        }else{
            imgpath=it.getStringExtra("imgpath");

            if(imgpath.equals("null")){
                Toast.makeText(this,"图片获取失败",Toast.LENGTH_SHORT).show();
            }else{
                photoView.setImageBitmap(BitmapFactory.decodeFile(imgpath));
            }
        }
        photoView.setOnClickListener(view -> finish());
    }
}
