package com.sjtuopennetwork.shareit.album;

import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import com.luck.picture.lib.PictureSelector;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.entity.LocalMedia;
import com.sjtuopennetwork.shareit.R;

import java.util.List;

import sjtu.opennet.honvideo.Segmenter;
import sjtu.opennet.honvideo.VideoMeta;


public class VideoActivity extends AppCompatActivity {
    // UI Gadget
    ImageView video_sync;
    ImageView video_add;

    List<LocalMedia> chooseVid;

    private static final String TAG = "VideoActivity";
    private static final int REQUEST_CODE_PICK_VIDEO = 42;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        initUI();
    }



    public void initUI(){
        video_sync=findViewById(R.id.video_sync);
        video_add=findViewById(R.id.video_add);
        video_sync.setOnClickListener(view -> {
            try{
                Segmenter.initFfmpeg(this);
            }catch(Exception e){
                e.printStackTrace();
            }
            //Segmenter.segment("aaa", "aaa");
        });
        video_add.setOnClickListener(video_add -> {
            Log.i(TAG, "Video Add Clicked.");
            PictureSelector.create(VideoActivity.this)
                    .openGallery(PictureMimeType.ofVideo())//全部.PictureMimeType.ofAll()、图片.ofImage()、视频.ofVideo()
                    .maxSelectNum(1)//最大选择数量
                    .compress(false)//是否压缩
                    .forResult(PictureConfig.CHOOSE_REQUEST);//结果回调 onActivityResult code

            //Segmenter.testFFresume(this);
        });
    }

    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode==PictureConfig.CHOOSE_REQUEST && resultCode==RESULT_OK){
            //选择或拍摄照片之后的回调，将对应图片添加到photo_thread中
            chooseVid=PictureSelector.obtainMultipleResult(data);
            String filePath=chooseVid.get(0).getPath();

            Log.i(TAG, String.format("Add video from file %s.", filePath));
            Log.i(TAG, "Extract video meta data");
            VideoMeta vMeta = new VideoMeta(filePath);
            vMeta.logCatPrint();
            Log.i(TAG, String.format("Try to stream video"));
            //Segmenter.getFrame(filePath);
            //Segmenter.testSegment(this, filePath);
        }
        else{
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

}
