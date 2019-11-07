package com.sjtuopennetwork.shareit.share;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.example.qrlibrary.qrcode.activity.DefaultQRScanActivity;
import com.sjtuopennetwork.shareit.R;
import com.sjtuopennetwork.shareit.contact.ContactQRCodeAtivity;
import com.sjtuopennetwork.shareit.contact.ScanResultActivity;

import sjtu.opennet.hon.Textile;

public class GroupQRCodeActivity extends DefaultQRScanActivity {


    @Override
    protected void initCustomViewAndEvents() {

    }

    @Override
    protected void onStart() {
        super.onStart();

        //去掉状态栏
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
    }

    @Override
    protected void onAlbumResult(int requestCode, int resultCode, String recode) {
        System.out.println("============相册得到结果："+recode);

        String[] decode=recode.split("/");

        //接受邀请
        try {
            Textile.instance().invites.acceptExternal(decode[0],decode[1]);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("==============相册扫码加群成功");
        finish();
    }

    @Override
    protected void handleDecodeResult(String rawResult, Bundle bundle) {
        System.out.println("===========扫码得到结果："+rawResult);

        String[] decode=rawResult.split("/");

        //接受邀请
        try {
            Textile.instance().invites.acceptExternal(decode[0],decode[1]);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("==============扫码加群成功");
        finish();
    }
}
