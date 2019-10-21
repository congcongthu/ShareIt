package com.sjtuopennetwork.shareit.login;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.sjtuopennetwork.shareit.R;
import com.sjtuopennetwork.shareit.share.HomeActivity;

public class MainActivity extends AppCompatActivity {

    //持久化存储
    SharedPreferences pref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main); 这个Activity不需要layout，因为直接使用华为的登录页面

        //查SharedPreference中"isLogin"判断登录状态，如果未登录则直接拉起华为ID登录界面。如果已登录则跳转到HomeActivity


        //华为ID登录后拿到昵称、头像并写入SharedPreference。

        //跳转到主界面
        Intent toHomeActivity=new Intent(this, HomeActivity.class);
        startActivity(toHomeActivity);
        finish();

    }
}
