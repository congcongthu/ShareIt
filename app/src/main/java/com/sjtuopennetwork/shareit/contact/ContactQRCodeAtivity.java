package com.sjtuopennetwork.shareit.contact;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.qrlibrary.qrcode.activity.DefaultQRScanActivity;

public class ContactQRCodeAtivity extends DefaultQRScanActivity {
    @Override
    protected void initCustomViewAndEvents() {

    }
    @Override
    protected void onStart() {
        super.onStart();
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
    }

    @Override
    protected void onAlbumResult(int requestCode, int resultCode, String recode) {
        Bundle bundle = new Bundle();
        bundle.putString("result",recode);
        System.out.println("============相册得到结果："+recode);
        startActivity(new Intent(ContactQRCodeAtivity.this,ScanResultActivity.class).putExtras(bundle));
//        finish();
    }

    @Override
    protected void handleDecodeResult(String rawResult, Bundle bundle) {
        bundle.putString("result",rawResult);
        System.out.println("===========扫码得到结果："+rawResult);
        startActivity(new Intent(ContactQRCodeAtivity.this,ScanResultActivity.class).putExtras(bundle));
//        finish();
    }
}
