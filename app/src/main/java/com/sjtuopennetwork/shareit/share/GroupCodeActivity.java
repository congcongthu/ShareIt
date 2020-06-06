package com.sjtuopennetwork.shareit.share;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import com.example.qrlibrary.qrcode.utils.QRCodeUtil;
import com.sjtuopennetwork.shareit.R;

import sjtu.opennet.hon.Textile;
import sjtu.opennet.textilepb.View;

public class GroupCodeActivity extends AppCompatActivity {

    private static final String TAG = "=====================";

    String threadID;
    String codeSource;
    String key;
    String inviteID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_code);

        threadID=getIntent().getStringExtra("threadid");
        System.out.println("================得到threadid："+threadID);

        try {
            View.ExternalInvite externalInvite = Textile.instance().invites.addExternal(threadID);
            key=externalInvite.getKey();
            inviteID=externalInvite.getId();
        } catch (Exception e) {
            e.printStackTrace();
        }

        codeSource=inviteID+"##"+key;
        Log.d(TAG, "onCreate: code:"+inviteID+"   "+key);
        ImageView imageView=findViewById(R.id.group_code);
        imageView.setImageBitmap(QRCodeUtil.CreateTwoDCode(codeSource));
    }
}
