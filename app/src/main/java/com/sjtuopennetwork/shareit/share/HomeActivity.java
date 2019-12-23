package com.sjtuopennetwork.shareit.share;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.design.bottomnavigation.LabelVisibilityMode;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.sjtuopennetwork.shareit.R;
import com.sjtuopennetwork.shareit.album.AlbumFragment;
import com.sjtuopennetwork.shareit.contact.ContactFragment;
import com.sjtuopennetwork.shareit.login.MainActivity;
import com.sjtuopennetwork.shareit.setting.SettingFragment;
import com.sjtuopennetwork.shareit.share.util.TDialog;
import com.sjtuopennetwork.shareit.share.util.TMsg;
import com.sjtuopennetwork.shareit.util.AppdbHelper;
import com.sjtuopennetwork.shareit.util.DBoperator;
import com.sjtuopennetwork.shareit.util.FileUtil;
import com.sjtuopennetwork.shareit.util.ForeGroundService;
import com.syd.oden.circleprogressdialog.core.CircleProgressDialog;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import sjtu.opennet.textilepb.Mobile;
import sjtu.opennet.textilepb.Model;
import sjtu.opennet.hon.BaseTextileEventListener;
import sjtu.opennet.hon.FeedItemData;
import sjtu.opennet.hon.FeedItemType;
import sjtu.opennet.hon.Handlers;
import sjtu.opennet.hon.Textile;
import sjtu.opennet.textilepb.View;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG = "====================";

    //UI控件
    private ShareFragment shareFragment;
    private SettingFragment settingFragment;
    private AlbumFragment albumFragment;
    private ContactFragment contactFragment;
    CircleProgressDialog circleProgressDialog;
    FragmentManager fragmentManager;
    FragmentTransaction transaction;

    //内存数据
    boolean nodeOnline=false;
    int login;


    //导航栏监听器，每次点击都进行fragment的切换
    private BottomNavigationView.OnNavigationItemSelectedListener navSeLis=new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
            if(isServiceRunning("com.sjtuopennetwork.shareit.util.ForeGroundService")){
                Log.d(TAG, "onCreate: 服务正在运行");
            }else{
                Log.d(TAG, "onCreate: 服务没有运行");
            }
            switch(menuItem.getItemId()){
                case R.id.share:
                    replaceFragment(shareFragment);
                    return true;
                case R.id.contacts:
                    replaceFragment(contactFragment);
                    return true;
//                case R.id.album:
//                    replaceFragment(albumFragment);
//                    return true;
                case R.id.setting:
                    replaceFragment(settingFragment);
                    return true;
            }
            return false;
        }
    };

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
//        super.onSaveInstanceState(outState, outPersistentState);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        login=getIntent().getIntExtra("login",5);
        Log.d(TAG, "跳转到HomeActivity，login："+login);
        if(login==0 || login==1 || login==2 || login==3){ //如果等于5，就不是登录页面跳转过来的
            initUI();

            if(!EventBus.getDefault().isRegistered(this)){
                EventBus.getDefault().register(this);
            }
            boolean serviceRunning=isServiceRunning("com.sjtuopennetwork.shareit.util.ForeGroundService");
            Log.d(TAG, "onCreate, service running :"+serviceRunning);
            if(serviceRunning){
                replaceFragment(shareFragment);
            }else{
                circleProgressDialog=new CircleProgressDialog(this);
                circleProgressDialog.setText("节点启动中");
                circleProgressDialog.showDialog();

                Intent intent=new Intent(this,ForeGroundService.class);
                intent.putExtra("login",login);
                startForegroundService(intent);
            }

        }
    }

    public boolean isServiceRunning(String ServiceName) {
        ActivityManager myManager = (ActivityManager) getApplicationContext()
                .getSystemService(Context.ACTIVITY_SERVICE);
        ArrayList<ActivityManager.RunningServiceInfo> runningService = (ArrayList<ActivityManager.RunningServiceInfo>) myManager
                .getRunningServices(30);

        for (int i = 0; i < runningService.size(); i++) {
            Log.d(TAG, "isServiceRunning: "+runningService.get(i).service.getClassName());
            if (runningService.get(i).service.getClassName()
                    .equals(ServiceName)) {
                return true;
            }
        }
        return false;
    }

    private void initUI(){
        Log.d(TAG, "initUI: 创建新的Fragment");
        shareFragment=new ShareFragment();
        contactFragment=new ContactFragment();
        albumFragment=new AlbumFragment();
        settingFragment=new SettingFragment();

        BottomNavigationView nav=findViewById(R.id.nav_view);
        nav.setLabelVisibilityMode(LabelVisibilityMode.LABEL_VISIBILITY_LABELED);
        nav.setOnNavigationItemSelectedListener(navSeLis);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void knowOnline(Integer integer){
        if(integer.intValue()==0){
            if(!nodeOnline){
                Log.d(TAG, "knowOnline: 主页得知节点启动了");
                circleProgressDialog.dismiss();
                nodeOnline=true;
                replaceFragment(shareFragment);
            }
        }
    }

    //切换Fragment
    private void replaceFragment(Fragment fragment) {
        fragmentManager = getSupportFragmentManager();
        transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.rep_layout, fragment);
        transaction.commitAllowingStateLoss();
    }


    @Override
    public void onStop() {
        super.onStop();

        if(EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().unregister(this);
        }
    }
}
