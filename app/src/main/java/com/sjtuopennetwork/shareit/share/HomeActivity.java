package com.sjtuopennetwork.shareit.share;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
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
import com.sjtuopennetwork.shareit.share.util.TDialog;
import com.sjtuopennetwork.shareit.share.util.TMsg;
import com.sjtuopennetwork.shareit.util.AppdbHelper;
import com.sjtuopennetwork.shareit.util.DBoperator;
import com.sjtuopennetwork.shareit.util.FileUtil;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import sjtu.opennet.textilepb.Model;
import sjtu.opennet.hon.BaseTextileEventListener;
import sjtu.opennet.hon.FeedItemData;
import sjtu.opennet.hon.FeedItemType;
import sjtu.opennet.hon.Handlers;
import sjtu.opennet.hon.Textile;

public class HomeActivity extends AppCompatActivity {

    //三个fragment
    private ShareFragment shareFragment;
    private SettingFragment settingFragment;
    private AlbumFragment albumFragment;
    private ContactFragment contactFragment;

    //持久化存储
    SharedPreferences pref;
    public SQLiteDatabase appdb;

    //内存数据
    String myname;
    String avatarpath;
    String phrase;

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

        initUI();
        initData();
    }

    private void getPermission() {
        System.out.println("=========输出");
        if(PermissionChecker.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)==PermissionChecker.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(this,
                    new String[]{"android.permission.WRITE_EXTERNAL_STORAGE",
                            "android.permission.READ_EXTERNAL_STORAGE",
                            "android.permission.CAMERA"},100);
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
        //初始化pref
        pref=getSharedPreferences("txtl",MODE_PRIVATE);
        appdb=AppdbHelper.getInstance(this).getWritableDatabase();
        myname=pref.getString("myname","null");
        avatarpath=pref.getString("avatarpath","null");
        phrase=pref.getString("openid","null");

        //初始化Textile
        Context ctx = getApplicationContext();
        final File filesDir = ctx.getFilesDir();
        final File repo = new File(filesDir, "textile-repo");
        final String repoPath = repo.getAbsolutePath();
        try {
            if (!Textile.isInitialized(repoPath)) { //如果未初始化

//                String phrase = Textile.newWallet(wordCount); //新建一个wallet
//                String seed = Textile.walletAccountAt(phrase, 0, "").getSeed(); //拿到第一个account的seed
//                Textile.initialize(repoPath, seed, true, false);

                //测试时创建一个新的账户
                Textile.initializeCreatingNewWalletAndAccount(repoPath,true, false);
            }
            Textile.launch(ctx, repoPath, true);
            Textile.instance().addEventListener(new MyTextileListener());
        } catch (Exception e) {
            e.printStackTrace();
        }

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
            //测试
            try {
                System.out.println("===================昵称："+Textile.instance().profile.name());
//                System.out.println("===================thread个数："+Textile.instance().threads.list().getItemsCount());
            } catch (Exception e) {
                e.printStackTrace();
            }

            //连网之后反馈给主界面
            EventBus.getDefault().postSticky(Integer.valueOf(0)); //会有先连网后启动ShareFragment的注册，所以用Sticky

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

//            try {
//                Textile.instance().ipfs.swarmConnect("/ip4/202.120.38.131/tcp/23524/ipfs/12D3KooWERhx7JQhFfXA3a7WGSPCH5Zd1EuQnY6eeQM3VrVUBg67");
//            } catch (Exception e) {
//                e.printStackTrace();
//            }

            }

        @Override
        public void contactQueryResult(String queryId, Model.Contact contact) {
            EventBus.getDefault().post(contact);
        }

        @Override
        public void threadAdded(String threadId) {
            System.out.println("============创建thread："+threadId);
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

            boolean isSingle=thread.getWhitelistCount()==2;
            if(feedItemData.type.equals(FeedItemType.JOIN)){
                System.out.println("=========收到JOIN消息，thread白名单个数："+thread.getWhitelistCount());
                String myAddr=Textile.instance().account.address();
                int whiteListCount=thread.getWhitelistCount();
                boolean keyIsMyAddress=thread.getKey().equals(myAddr); //如果key是自己的address，说明这是同意他人的好友申请
                boolean authorIsMe=feedItemData.join.getUser().getAddress().equals(myAddr); //表明是否是自己的JOIN

                if(whiteListCount==2){ //双人thread
                    System.out.println("===========JOIN消息，白名单个数为2");
                    if(!authorIsMe){  //双人thread收到他人的JOIN，只可能是同意他人好友申请或者自己的好友申请被他人同意，都要插入一条记录
                        System.out.println("============白名单数为2的JOIN："+feedItemData.join.getUser().getName());
                        TDialog tDialog=DBoperator.insertDialog(appdb, threadId, feedItemData.join.getUser().getName(),
                                "你好啊，现在我们已经成为好友了",
                                feedItemData.join.getDate().getSeconds(),
                                0, //后台收到默认是未读的
                                feedItemData.join.getUser().getAvatar(),
                                1, 1);
                        EventBus.getDefault().post(tDialog);

                        TMsg tMsg=DBoperator.insertMsg(appdb,threadId,0, feedItemData.join.getBlock(),feedItemData.join.getUser().getName(),
                                feedItemData.join.getUser().getAvatar(),
                                feedItemData.join.getUser().getName()+"你好啊，现在我们已经成为好友了",
                                feedItemData.join.getDate().getSeconds(), 0);
                        EventBus.getDefault().post(tMsg);
                    }else{
                        System.out.println("==========================自己的白名单数为2的JOIN");
                    }
                }else{//收到群组thread，就先查一下看数据库有没有，没有就插入，有就更新。
                    System.out.println("==================群组JOIN："+thread.getName());
                    TDialog tDialog=DBoperator.queryDialogByThreadID(appdb,threadId);
                    TDialog updateDialog=null;
                    if(tDialog!=null){ //如果数据库已经有了，就更新
                        System.out.println("================得到群组JOIN消息"+feedItemData.join.getUser().getName()+" 加入了群组");
                        updateDialog=DBoperator.dialogGetMsg(appdb,tDialog,threadId,
                                feedItemData.join.getUser().getName()+" 加入了群组", feedItemData.join.getDate().getSeconds(),
                                tDialog.imgpath);
                    }else{ //如果数据库没有就插入
                        System.out.println("================得到群组JOIN消息，创建群组"+feedItemData.join.getUser()+" 加入了群组");
                        updateDialog=DBoperator.insertDialog(appdb, threadId, thread.getName(),
                                feedItemData.join.getUser().getName()+" 加入了群组",
                                feedItemData.join.getDate().getSeconds(),
                                0,
                                feedItemData.join.getUser().getAvatar(),
                                0, 1);
                    }
                    int ismine=0;
                    if(feedItemData.text.getUser().getAddress().equals(Textile.instance().account.address())){
                        ismine=1;
                    }
                    TMsg tMsg=DBoperator.insertMsg(appdb,threadId,0, feedItemData.join.getBlock(),
                            feedItemData.join.getUser().getName(),
                            feedItemData.join.getUser().getAvatar(),
                            feedItemData.join.getUser().getName()+" 加入了群组",
                            feedItemData.join.getDate().getSeconds(), ismine);

                    EventBus.getDefault().post(updateDialog);
                    EventBus.getDefault().post(tMsg);
                }
            }

            if(feedItemData.type.equals(FeedItemType.TEXT)){ //如果是文本消息
                System.out.println("=================收到文本消息："+feedItemData.text.getBody());
                int ismine=0;
                if(feedItemData.text.getUser().getAddress().equals(Textile.instance().account.address())){
                    ismine=1;
                }
                //插入msgs表
                TMsg tMsg=DBoperator.insertMsg(appdb,threadId,0, feedItemData.text.getBlock(),
                        feedItemData.text.getUser().getName(),
                        feedItemData.text.getUser().getAvatar(),
                        feedItemData.text.getBody(),
                        feedItemData.text.getDate().getSeconds(), ismine);
                EventBus.getDefault().post(tMsg);

                //更新dialogs表
                TDialog tDialog=DBoperator.queryDialogByThreadID(appdb,threadId);
                TDialog updateDialog=DBoperator.dialogGetMsg(appdb,tDialog,threadId,
                        feedItemData.text.getBody(), feedItemData.text.getDate().getSeconds(),
                        tDialog.imgpath);
                EventBus.getDefault().post(updateDialog);

            }

            if(feedItemData.type.equals(FeedItemType.FILES)){
                //在这里就不存储图片，直接拿到hash放进去，adapter中进行展示。因为有可能老消息不需要展示，等到需要时再去拉
                //如果是双人的不更新Dialog的图片，如果是群聊就要更新
                String large_hash="";
                try {
                    large_hash = Textile.instance().files.list(threadId,"",3).getItems(0).getFiles(0).getLinksMap().get("large").getHash();
                    System.out.println("======================图片消息的hash值："+large_hash);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                //DIalog
                TDialog tDialog=DBoperator.queryDialogByThreadID(appdb,threadId);
                String dialogimg="";
                if(isSingle){ //单人的thread
                    dialogimg=tDialog.imgpath;
                }else{ //多人的群组
                    dialogimg=large_hash;
                }
                System.out.println("================是否是单人的："+isSingle+" "+dialogimg);
                TDialog updateDialog=DBoperator.dialogGetMsg(appdb,tDialog,threadId,
                        feedItemData.files.getUser().getName()+"分享了图片", feedItemData.files.getDate().getSeconds(),
                        dialogimg);
                System.out.println("====================这个执行了吗");

                //Msg
                int ismine=0;
                if(feedItemData.files.getUser().getAddress().equals(Textile.instance().account.address())){
                    ismine=1;
                }
                //插入msgs表
                TMsg tMsg=DBoperator.insertMsg(appdb,threadId,1, feedItemData.files.getBlock(),
                        feedItemData.files.getUser().getName(),
                        feedItemData.files.getUser().getAvatar(),
                        large_hash,
                        feedItemData.files.getDate().getSeconds(), ismine);
                System.out.println("=============msgs：" + tMsg.authorname+" " + tMsg.authorname);

                EventBus.getDefault().post(updateDialog);
                EventBus.getDefault().post(tMsg);
            }
        }
    }

}
