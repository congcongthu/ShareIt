package com.sjtuopennetwork.shareit.share;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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
import android.util.Pair;
import android.view.MenuItem;

import com.sjtuopennetwork.shareit.R;
import com.sjtuopennetwork.shareit.album.AlbumFragment;
import com.sjtuopennetwork.shareit.contact.ContactFragment;
import com.sjtuopennetwork.shareit.setting.SettingFragment;
import com.sjtuopennetwork.shareit.util.AppdbHelper;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import io.textile.pb.Model;
import io.textile.textile.BaseTextileEventListener;
import io.textile.textile.FeedItemData;
import io.textile.textile.FeedItemType;
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
    private AppdbHelper appdbHelper;
    public SQLiteDatabase appdb;

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

        getPermission();

        isFirstRun=getIntent().getBooleanExtra("isFirstRun",true);

        initUI();

        initData();
    }

    private void getPermission() {
        if(PermissionChecker.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)==PermissionChecker.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(this,
                    new String[]{"android.permission.WRITE_EXTERNAL_STORAGE",
                            "android.permission.READ_EXTERNAL_STORAGE"},100);
        }
    }

    private void initData() {
        //初始化pref
        pref=getSharedPreferences("txtl",MODE_PRIVATE);
        appdbHelper=new AppdbHelper(HomeActivity.this,"txtl.db",null,1);
        appdb=appdbHelper.getWritableDatabase();

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

            //节点连网之后，如果是首次运行要设置昵称和头像，昵称和头像是登录时华为ID得到，已经存在pref中
            if(isFirstRun){
                String myname=pref.getString("myname","ceshi1"); //这里测试使用ceshi1
                String avatarpath=pref.getString("avatarpath","null");
                try {
                    Textile.instance().profile.setName(myname);
                    Textile.instance().profile.setAvatar(avatarpath, new Handlers.BlockHandler() {
                        @Override
                        public void onComplete(Model.Block block) {
                            System.out.println("======头像设置成功");
                        }

                        @Override
                        public void onError(Exception e) {
                            System.out.println("======头像设置失败");
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            Textile.instance().cafes.register(
                    "http://202.120.38.131:40601",
                    "24NR6PTk3ocFCxqidUHWAi6nmhcc76DzMgWHkcMYryeQ8YGRVZmXeLKkx1yXS",
                    new Handlers.ErrorHandler() {
                        @Override
                        public void onComplete() {
                            System.out.println("==========cafe连接成功");

                            //写到Share...
                        }
                        @Override
                        public void onError(Exception e) {
                            System.out.println("==========cafe连接失败");
                            //写到Share...
                        }
                    });

            //连网之后反馈给主界面
            EventBus.getDefault().postSticky(Integer.valueOf(0)); //会有先连网后启动ShareFragment的注册，所以用Sticky
        }

        @Override
        public void contactQueryResult(String queryId, Model.Contact contact) {
            EventBus.getDefault().post(contact);
        }

        @Override
        public void threadAdded(String threadId) {
            Pair<Integer,String> addFriend=new Pair<>(1,threadId);
            EventBus.getDefault().post(addFriend); //只在添加联系人的时候起作用，创建群组的时候要过滤掉
        }

        @Override
        public void threadUpdateReceived(String threadId, FeedItemData feedItemData) {
            //要保证在所有界面收到消息，就只能是在这里更新数据库了。默认是未读的，但是在聊天界面得到消息就要改为已读
            //发送消息的目的就是更新界面，所以不用sticky
            Model.Thread thread=null;
            try {
                thread=Textile.instance().threads.get(threadId);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if(feedItemData.type.equals(FeedItemType.JOIN)){
                String myAddr=Textile.instance().account.address();
                int whiteListCount=thread.getWhitelistCount();
                boolean keyIsMyAddress=thread.getKey().equals(myAddr); //如果key是自己的address，说明这是同意他人的好友申请
                boolean authorIsMe=feedItemData.join.getUser().getAddress().equals(myAddr); //表明是否是自己的JOIN

                if(whiteListCount==2){ //双人thread
                    if(keyIsMyAddress){ //同意他人的好友申请，会收到自己的和他人的JOIN

                    }else{
                        if(authorIsMe){ //向他人发送好友申请，会收到自己的JOIN，

                        }else{ //自己的好友申请被他人同意，会收到他人的JOIN

                        }
                    }

                }else{ //群组thread
                    if(authorIsMe){ //自己加入群组，就是创建了一个新群组，需要向dialogs表insert

                    }else{ //他人加入群组，需要将dialogs表update

                    }
                }





            }




        }
    }

}
