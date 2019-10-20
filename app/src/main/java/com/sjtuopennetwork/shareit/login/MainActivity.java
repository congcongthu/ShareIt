package com.sjtuopennetwork.shareit.login;

import android.content.Intent;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.sjtuopennetwork.shareit.R;
import com.sjtuopennetwork.shareit.share.HomeActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main); 这个Activity不需要layout，因为直接使用华为的登陆页面

        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);

        //查SharedPreference中"isLogin"判断登录状态，如果未登录则直接拉起华为ID登录界面。如果已登录则跳转到HomeActivity


        //本来是先要用华为ID登录，拿到昵称、头像并写入SharedPreference之后跳转。这里就先直接跳转了。
        Intent toHomeActivity=new Intent(this, HomeActivity.class);
        startActivity(toHomeActivity);
        finish();

    }
}
