package com.sjtuopennetwork.shareit.util;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import com.example.qrlibrary.qrcode.activity.DefaultQRScanActivity;
import com.sjtuopennetwork.shareit.contact.ScanResultActivity;

import sjtu.opennet.hon.Textile;

public class QRCodeActivity extends DefaultQRScanActivity {

    SharedPreferences pref;

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

        pref=getSharedPreferences("txtl",MODE_PRIVATE);
    }

    @Override
    protected void onAlbumResult(int requestCode, int resultCode, String recode) {
        System.out.println("============相册得到结果："+recode);

        String[] decode=recode.split("##");
        if(decode.length==1) { //影子swarm
            try {
                boolean s1=Textile.instance().ipfs.swarmConnect(decode[0]);
                if(s1){
                    SharedPreferences.Editor editor=pref.edit();
                    editor.putString("shadowSwarm",decode[0]);
                    editor.commit();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            finish();
        }else if(decode.length==2){ //那就是群组
            //接受邀请
            try {
                Textile.instance().invites.acceptExternal(decode[0],decode[1]);
            } catch (Exception e) {
                e.printStackTrace();
            }

            System.out.println("==============扫码加群成功");
            finish();
        }else if(decode.length==3) { //那就是好友
            Bundle bundle=new Bundle();
            bundle.putString("result",recode);
            startActivity(new Intent(QRCodeActivity.this,ScanResultActivity.class).putExtras(bundle));
            finish();
        }
    }

    @Override
    protected void handleDecodeResult(String rawResult, Bundle bundle) {
        System.out.println("===========扫码得到结果："+rawResult);

        String[] decode=rawResult.split("##");

        if(decode.length==1) { //影子swarm
            try {
                boolean s1=Textile.instance().ipfs.swarmConnect(decode[0]);
                if(s1){
                    SharedPreferences.Editor editor=pref.edit();
                    editor.putString("shadowSwarm",decode[0]);
                    editor.commit();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            finish();
        }else if(decode.length==2){ //那就是群组
            //接受邀请
            try {
                Textile.instance().invites.acceptExternal(decode[0],decode[1]);
            } catch (Exception e) {
                e.printStackTrace();
            }

            System.out.println("==============扫码加群成功");
            finish();
        }else if(decode.length==3) { //那就是好友
            bundle.putString("result",rawResult);
            startActivity(new Intent(QRCodeActivity.this,ScanResultActivity.class).putExtras(bundle));
            finish();
        }
    }
}
