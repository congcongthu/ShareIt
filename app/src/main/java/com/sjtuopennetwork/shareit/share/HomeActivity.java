package com.sjtuopennetwork.shareit.share;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.design.bottomnavigation.LabelVisibilityMode;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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
            switch(menuItem.getItemId()){
                case R.id.share:
                    replaceFragment(shareFragment);
                    return true;
                case R.id.contacts:
                    replaceFragment(contactFragment);
                    return true;
                case R.id.album:
                    replaceFragment(albumFragment);
                    return true;
                case R.id.setting:
                    replaceFragment(settingFragment);
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        circleProgressDialog=new CircleProgressDialog(this);
        circleProgressDialog.setText("节点启动中");
        circleProgressDialog.showDialog();

        initUI();

        if(!EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().register(this);
        }

        try{
            if(Textile.instance().online()){ //如果节点已经上线了，就不进行初始化了，直接显示
                circleProgressDialog.dismiss();
                nodeOnline=true;
                replaceFragment(shareFragment);
            }
        }catch (Exception e){
            initData();
        }
    }

    private void initUI(){
        shareFragment=new ShareFragment();
        contactFragment=new ContactFragment();
        albumFragment=new AlbumFragment();
        settingFragment=new SettingFragment();

        BottomNavigationView nav=findViewById(R.id.nav_view);
        nav.setLabelVisibilityMode(LabelVisibilityMode.LABEL_VISIBILITY_LABELED);
        nav.setOnNavigationItemSelectedListener(navSeLis);
    }

    private void initData() {
        //持久化存储
//        pref=getSharedPreferences("txtl",MODE_PRIVATE);
        login=getIntent().getIntExtra("login",0);

        Intent intent=new Intent(this,ForeGroundService.class);
        intent.putExtra("login",login);
        startForegroundService(intent);
    }

    @Subscribe(threadMode = ThreadMode.MAIN,sticky = true)
    public void knowOnline(Integer integer){
        if(integer.intValue()==0){
            System.out.println("=========nodeOnline:"+nodeOnline);
            if(!nodeOnline){
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
}
