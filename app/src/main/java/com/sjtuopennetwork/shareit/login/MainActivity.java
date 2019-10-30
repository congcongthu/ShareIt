package com.sjtuopennetwork.shareit.login;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import com.sjtuopennetwork.shareit.share.HomeActivity;

public class MainActivity extends AppCompatActivity {

    //持久化存储
    SharedPreferences pref;

    //内存数据
    boolean isLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pref=getSharedPreferences("txtl",MODE_PRIVATE);

        //查SharedPreference中"isLogin"判断登录状态，如果未登录则直接拉起华为ID登录界面。如果已登录则跳转到HomeActivity
        isLogin=pref.getBoolean("isLogin",false); //如果没有这个字段就是首次打开
        if(isLogin){ //如果已经登录直接跳转到主界面
            Intent toHomeActivity=new Intent(this, HomeActivity.class);
            startActivity(toHomeActivity);
            finish();
        }else{ //如果未登录
            // TODO: 2019/10/28  在这里拉起华为登录页面

            //如果有了华为ID登录，这一块代码要删掉
            String myname="jaspq";
            String avatarpath="null";
            String openid="null";

            //写入SharedPreference
            SharedPreferences.Editor editor=pref.edit();
            editor.putString("myname",myname);
            editor.putString("avatarpath",avatarpath);
            editor.putString("openid",openid);
            editor.putBoolean("isLogin",true);
            editor.commit();

            Intent toHomeActivity=new Intent(this, HomeActivity.class);
            startActivity(toHomeActivity);
            finish();

        }
    }

    //从华为ID返回到结果之后，将结果写入到SharedPreference
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        // TODO: 2019/10/28 将华为ID的头像存储到文件，并将用户名、头像路径、openid存储到下面这三个变量中
        String myname="null";
        String avatarpath="null";
        String openid="null";

        //写入SharedPreference
        SharedPreferences.Editor editor=pref.edit();
        editor.putString("myname",myname);
        editor.putString("avatarpath",avatarpath);
        editor.putString("openid",openid);
        editor.putBoolean("isLogin",true);
        editor.commit();

        Intent toHomeActivity=new Intent(this, HomeActivity.class);
        startActivity(toHomeActivity);
        finish();
    }
}
