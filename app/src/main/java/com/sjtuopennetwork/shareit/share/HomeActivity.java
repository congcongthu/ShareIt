package com.sjtuopennetwork.shareit.share;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.design.bottomnavigation.LabelVisibilityMode;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;

import com.sjtuopennetwork.shareit.R;
import com.sjtuopennetwork.shareit.album.AlbumFragment;
import com.sjtuopennetwork.shareit.contact.ContactFragment;
import com.sjtuopennetwork.shareit.setting.SettingFragment;
import com.sjtuopennetwork.shareit.util.MyEvent;

import org.greenrobot.eventbus.EventBus;

import java.io.File;

import io.textile.pb.Model;
import io.textile.textile.BaseTextileEventListener;
import io.textile.textile.Handlers;
import io.textile.textile.Textile;

public class HomeActivity extends AppCompatActivity {

    //三个fragment
    private ShareFragment shareFragment;
    private SettingFragment settingFragment;
    private AlbumFragment albumFragment;
    private ContactFragment contactFragment;

    //持久化存储
    SharedPreferences pref;

    //内存数据
    boolean isFirstRun;

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

        isFirstRun=getIntent().getBooleanExtra("isFirstRun",true);

        initUI();

        initData();

    }

    private void initData() {

        //初始化pref
        pref=getSharedPreferences("txtl",MODE_PRIVATE);

        //初始化Textile
        Context ctx = getApplicationContext();
        final File filesDir = ctx.getFilesDir();
        final File repo = new File(filesDir, "textile-repo");
        final String repoPath = repo.getAbsolutePath();
        try {
            if (!Textile.isInitialized(repoPath)) { //如果未初始化
                long wordCount = pref.getLong("wordCount", 0); //获得phrase的词数
                if (wordCount != 1) { //应该是不等于0，但是为了测试可以进入if，这里不等于1
//                    String phrase = Textile.newWallet(wordCount); //新建一个wallet
//                    String seed = Textile.walletAccountAt(phrase, 0, "").getSeed(); //拿到第一个account的seed
//                    Textile.initialize(repoPath, seed, true, false);

                    //测试时创建一个新的账户
                    Textile.initializeCreatingNewWalletAndAccount(repoPath,true, false);
                }
            }
            Textile.launch(ctx, repoPath, true);
            Textile.instance().addEventListener(new MyTextileListener());
        } catch (Exception e) {
            e.printStackTrace();
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

        replaceFragment(shareFragment);
    }

    //切换Fragment
    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.rep_layout, fragment);
        transaction.commit();
    }

    class MyTextileListener extends BaseTextileEventListener {
        @Override
        public void nodeOnline() {
            super.nodeOnline();

            //节点连网之后，如果是首次运行要设置昵称和头像，昵称和头像是登录时华为ID得到，已经存在pref中
            if(isFirstRun){
                String myname=pref.getString("myname","ceshi1"); //这里测试使用ceshi1
                String avatarpath=pref.getString("avatarpath","null");
                try {
                    Textile.instance().profile.setName(myname);
                    Textile.instance().profile.setAvatar(avatarpath, new Handlers.BlockHandler() {
                        @Override
                        public void onComplete(Model.Block block) {

                        }

                        @Override
                        public void onError(Exception e) {

                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            //连网之后反馈给主界面
            EventBus.getDefault().postSticky(new MyEvent(0,null)); //怕先连网，后启动ShareFragment的注册，所以用Sticky
        }
    }
}
