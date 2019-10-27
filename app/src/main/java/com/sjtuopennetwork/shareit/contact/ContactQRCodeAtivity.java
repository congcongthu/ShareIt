package com.sjtuopennetwork.shareit.contact;

import android.content.Intent;
import android.os.Bundle;

import com.example.qrlibrary.qrcode.activity.DefaultQRScanActivity;

public class ContactQRCodeAtivity extends DefaultQRScanActivity {
    @Override
    protected void initCustomViewAndEvents() {

    }

    @Override
    protected void onAlbumResult(int requestCode, int resultCode, String recode) {
        Bundle bundle = new Bundle();
        bundle.putString("result",recode);
        startActivity(new Intent(ContactQRCodeAtivity.this,ScanResultActivity.class).putExtras(bundle));
        finish();
    }

    @Override
    protected void handleDecodeResult(String rawResult, Bundle bundle) {
        bundle.putString("result",rawResult);
        startActivity(new Intent(ContactQRCodeAtivity.this,ScanResultActivity.class).putExtras(bundle));
        finish();
    }
}
