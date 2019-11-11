package com.sjtuopennetwork.shareit.album;

import android.content.Intent;
import android.graphics.Bitmap;
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
import sjtu.opennet.textilepb.Model;

import com.sjtuopennetwork.shareit.util.FileUtil;
import com.sjtuopennetwork.shareit.util.VideoHelper;

public class VideoActivity extends AppCompatActivity {
    // UI Gadget
    ImageView video_sync;
    ImageView video_add;
    ImageView video_delete;
    List<LocalMedia> chooseVid;

    private static final String TAG = "VideoActivity";
    private String videoDir;
    // videoDir can not be static as we need VideoActivity.context

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        //Initialize ffmpeg binary
        try{
            Segmenter.initFfmpeg(this);
        }catch(Exception e){
            e.printStackTrace();
        }
        //Initialize video directory.
        videoDir = FileUtil.getAppExternalPath(this, "video");

        //Initialize UI gadget.
        initUI();
    }

    public void initUI(){
        video_sync=findViewById(R.id.video_sync);
        video_add=findViewById(R.id.video_add);
        video_delete=findViewById(R.id.video_delete);
        //Listener for video sync
        video_sync.setOnClickListener(view -> {

            String testpath = FileUtil.getAppExternalPath(this,"");
            Log.i(TAG, String.format("External storage path %s", testpath));
            //Segmenter.segment("aaa", "aaa");
        });

        //Listener for video add
        video_add.setOnClickListener(video_add -> {
            Log.i(TAG, "Video Add Clicked.");
            PictureSelector.create(VideoActivity.this)
                    .openGallery(PictureMimeType.ofVideo())//全部.PictureMimeType.ofAll()、图片.ofImage()、视频.ofVideo()
                    .maxSelectNum(1)//最大选择数量
                    .compress(false)//是否压缩
                    .forResult(PictureConfig.CHOOSE_REQUEST);//结果回调 onActivityResult code
            //Segmenter.testFFresume(this);
        });

        //Listener for video delete
        video_delete.setOnClickListener(view->{
            VideoHelper.cleanAll(this);
        });
    }

    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == PictureConfig.CHOOSE_REQUEST && resultCode == RESULT_OK) {
            chooseVid = PictureSelector.obtainMultipleResult(data);
            String filePath = chooseVid.get(0).getPath();

            //ThreadAddVideo

            //


            Log.d(TAG, String.format("Add video from file %s.", filePath));
            Log.d(TAG, "Meta get start");
            long startTime = System.currentTimeMillis();
            //VideoMeta vMeta = new VideoMeta(filePath);
            //vMeta.logCatPrint();
            VideoHelper vHelper = new VideoHelper(this, filePath);
            long endTime = System.currentTimeMillis();
            Log.d(TAG, "Meta get end");
            Log.d(TAG, String.format("Meta get time %d ms", endTime - startTime));
            Log.d(TAG, "Try to publish video meta");
            startTime = System.currentTimeMillis();
            vHelper.publishMeta();
            endTime = System.currentTimeMillis();
            Log.d(TAG, String.format("Meta publish time %d ms", endTime - startTime));
            Log.d(TAG, "Try to stream video");
            vHelper.segment();

            /*
            Log.d(TAG, "Try to receive thumbnail from ipfs");
            Model.Video tmpVpb = vHelper.getVideoPb();
            Bitmap tmpBmap = VideoHelper.getPosterFromPb(tmpVpb);
            String tmpdir = FileUtil.getAppExternalPath(this, "temp");
            VideoHelper.saveBitmap(tmpBmap, String.format("%s/receivedThumbnail.png", tmpdir));
*           /

            //Segmenter.getFrame(filePath);
            //Segmenter.testSegment(this, filePath);
            /*
            Log.i(TAG, String.format("Try to get video info."));
            try{
                Segmenter.getInfo(this, filePath);
            }catch(Exception e){
                e.printStackTrace();
            }

             */
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }


    private void doVideoAdd(){

    }
}
