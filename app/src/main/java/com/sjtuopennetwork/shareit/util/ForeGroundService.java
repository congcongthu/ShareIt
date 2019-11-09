package com.sjtuopennetwork.shareit.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import com.sjtuopennetwork.shareit.R;
import com.sjtuopennetwork.shareit.share.HomeActivity;
import com.sjtuopennetwork.shareit.share.util.TDialog;
import com.sjtuopennetwork.shareit.share.util.TMsg;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.List;
import java.util.UUID;

import sjtu.opennet.hon.BaseTextileEventListener;
import sjtu.opennet.hon.FeedItemData;
import sjtu.opennet.hon.FeedItemType;
import sjtu.opennet.hon.Handlers;
import sjtu.opennet.hon.Textile;
import sjtu.opennet.textilepb.Mobile;
import sjtu.opennet.textilepb.Model;
import sjtu.opennet.textilepb.QueryOuterClass;
import sjtu.opennet.textilepb.View;

public class ForeGroundService extends Service {
    public ForeGroundService() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    String loginAccount;
    int login;
    String repoPath;
    SharedPreferences pref;
    SQLiteDatabase appdb;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        login=intent.getIntExtra("login",0);
        pref=getSharedPreferences("txtl",MODE_PRIVATE);
        repoPath=intent.getStringExtra("repopath");

        new Thread(){
            @Override
            public void run() {
                super.run();
                System.out.println("========启动前台服务");
                NotificationManager notificationManager=(NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                NotificationChannel notificationChannel=new NotificationChannel("12","前台服务",NotificationManager.IMPORTANCE_HIGH);
                notificationManager.createNotificationChannel(notificationChannel);
                Notification notification=new Notification.Builder(ForeGroundService.this,"12")
                        .setContentText("为正常接收消息，请保持ShareIt在后台运行")
                        .setContentTitle("ShareIt正在运行")
                        .setSmallIcon(R.drawable.ic_share_launch)
                        .build();
                startForeground(108,notification);

                initTextile(login);
            }
        }.start();

        if(!EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().register(this);
        }

        return super.onStartCommand(intent, flags, startId);
    }

    public void initTextile(int login){
        String phrase="";
//        Context ctx = getApplicationContext();
        final File filesDir = this.getFilesDir();

        switch (login){
            case 0: //已经登录，找到repo，初始化textile
                System.out.println("========已经登录");
                loginAccount=pref.getString("loginAccount","null"); //当前登录的account，就是address
                final File repo0 = new File(filesDir, loginAccount);
                repoPath = repo0.getAbsolutePath();
                break;
            case 1: //shareit注册，新建repo，初始化textile
                System.out.println("===============shareit注册");
                try {
                    phrase= Textile.newWallet(12);
                    Mobile.MobileWalletAccount m=Textile.walletAccountAt(phrase,Textile.WALLET_ACCOUNT_INDEX,Textile.WALLET_PASSPHRASE);
                    loginAccount=m.getAddress();
                    final File repo1 = new File(filesDir, loginAccount);
                    repoPath = repo1.getAbsolutePath();
                    Textile.initialize(repoPath,m.getSeed() , true, false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case 2: //华为账号登录，找到repo，初始化textile
                System.out.println("========华为账号登录");
                String openid=pref.getString("openid",""); //?测试一下是否需要截断，应该并不需要
                String avatarUri=pref.getString("avatarUri",""); //先判断一下是否已经存储过
                if(FileUtil.getFilePath(avatarUri).equals("null")){
                    //新开线程获得头像
                    new Thread(){
                        @Override
                        public void run() {
                            String huaweiAvatar=FileUtil.getHuaweiAvatar(avatarUri);
                            SharedPreferences.Editor editor=pref.edit();
                            editor.putString("avatarpath",huaweiAvatar);
                            editor.commit();
                        }
                    }.start();
                }
                try {
                    phrase=Textile.newWalletFromHuaweiOpenId(openid);
                    Mobile.MobileWalletAccount m=Textile.walletAccountAt(phrase,Textile.WALLET_ACCOUNT_INDEX,Textile.WALLET_PASSPHRASE);
                    loginAccount=m.getAddress();
                    final File repo1 = new File(filesDir, loginAccount);
                    repoPath = repo1.getAbsolutePath();
                    if(!Textile.isInitialized(repoPath)){
                        Textile.initialize(repoPath,m.getSeed() , true, false);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case 3: //shareit助记词登录，初始化textile
                System.out.println("========助记词登录");
                phrase=pref.getString("phrase","");
                loginAccount=pref.getString("loginAccount","");
                System.out.println("===============助记词："+phrase);
                break;
        }

        //启动Textile
        try {
            Textile.launch(ForeGroundService.this, repoPath, true);
            Textile.instance().addEventListener(new MyTextileListener());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SharedPreferences.Editor editor=pref.edit();
        editor.putBoolean("isLogin",true);
        if(login==1 || login ==2){ //1,2都需要修改助记词和登录账户,3不需要
            editor.putString("phrase",phrase);
            editor.putString("loginAccount",loginAccount);
        }
        editor.commit();


        try{
            appdb=AppdbHelper.getInstance(this,loginAccount).getWritableDatabase();
        }catch (Exception e){
//            finish();
            e.printStackTrace();
        }

        System.out.println("==================登录账户："+loginAccount+" "+phrase);
    }

    @Subscribe(threadMode = ThreadMode.MAIN,sticky = true)
    public void tryConnectCafe(Integer integer){
        if(integer==9){
            Textile.instance().cafes.register(
                    "http://202.120.38.131:40601",
                    "2HgCmu3Gk72XkVbccXGA14JehiwL9tfT9tLueVc1FTRhX1UJzZRX9vhy1dkTB",
                    new Handlers.ErrorHandler() {
                        @Override
                        public void onComplete() {
                            System.out.println("==========131cafe连接成功");
                            QueryOuterClass.QueryOptions options = QueryOuterClass.QueryOptions.newBuilder().build();
                            try {
                                Textile.instance().account.sync(options);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        @Override
                        public void onError(Exception e) {
                            System.out.println("==========131cafe连接失败");
                            //发消息再连接
                            EventBus.getDefault().post(new Integer(9));
                        }
                    });
        }
    }

    class MyTextileListener extends BaseTextileEventListener {
        @Override
        public void nodeOnline() {

            QueryOuterClass.QueryOptions options = QueryOuterClass.QueryOptions.newBuilder().build();
            try {
                Textile.instance().account.sync(options);
            } catch (Exception e) {
                e.printStackTrace();
            }

            //测试通知
            try {
                List<Model.Notification> noti_list= Textile.instance().notifications.list("",500).getItemsList();
                System.out.println("============通知条数："+noti_list.size());
                for(Model.Notification n:noti_list){
                    System.out.println("=======通知的内容："+n.getType().toString()+" "+n.getUser().getName()+" "+n.getBody());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            createDeviceThread();

            tryConnectCafe(9);

//            try {
//                Textile.instance().ipfs.swarmConnect("/ip4/202.120.38.131/tcp/23524/ipfs/12D3KooWERhx7JQhFfXA3a7WGSPCH5Zd1EuQnY6eeQM3VrVUBg67");
//            } catch (Exception e) {
//                e.printStackTrace();
//            }

            System.out.println("=====online-middle");

//            根据登录方式，设置name和头像
            System.out.println("=========login:"+login);
            switch(login){
                case 0: //已经登录过的，不用设置name、avatar
                    break;
                case 1: //shareit注册，每次都要设置
                    String shareitName=pref.getString("myname","");
                    String shareitAvatarpath=pref.getString("avatarpath","");
                    System.out.println("=====用户名和头像："+shareitName+" "+shareitAvatarpath);
                    try {
                        Textile.instance().profile.setName(shareitName);
                        if(!shareitAvatarpath.equals("")){
                            Textile.instance().profile.setAvatar(shareitAvatarpath, new Handlers.BlockHandler() {
                                @Override
                                public void onComplete(Model.Block block) {
                                    System.out.println("==========shareit注册设置头像成功");
                                    QueryOuterClass.QueryOptions options = QueryOuterClass.QueryOptions.newBuilder().build();
                                    try {
                                        Textile.instance().account.sync(options);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }

                                @Override
                                public void onError(Exception e) {
                                    System.out.println("==========shareit注册设置头像失败");
                                }
                            });
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case 2: //华为ID登录，判断Textile实例之后进行设置
                    try {
                        String huaweiName=pref.getString("myname","");
                        if(!Textile.instance().profile.name().equals(huaweiName)){ //首次用华为id登录还没有设置Textile账号
                            Textile.instance().profile.setName(huaweiName);
                            System.out.println("==========华为ID的name："+huaweiName);
                        }
                        String huaweiAvatarpath=pref.getString("avatarpath","");
                        if(!Textile.instance().profile.avatar().equals(huaweiAvatarpath)){
                            System.out.println("===========华为id的avatar："+huaweiAvatarpath);
                            if(!huaweiAvatarpath.equals("")){ //后台线程已经拿到头像
                                Textile.instance().profile.setAvatar(huaweiAvatarpath, new Handlers.BlockHandler() {
                                    @Override
                                    public void onComplete(Model.Block block) {
                                        System.out.println("==========华为ID登录设置头像成功");
                                    }

                                    @Override
                                    public void onError(Exception e) {
                                        System.out.println("==========华为ID登录设置头像失败");
                                    }
                                });
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case 3: //助记词登录，理论上要判断后重新进行设置，新的peer的name到底能不能同步过来？
                    try {
                        if(Textile.instance().profile.name().equals("")){ //如果没有同步过来，
                            System.out.println("=============助记词登录，新的name没有同步过来");
                        }else{
                            //put到myname中
                        }
                        if(Textile.instance().profile.avatar().equals("")) { //如果没有同步过来，
                            System.out.println("=============助记词登录，新的avatar没有同步过来");
                        }else{
                            //获得头像，并将路径put到avatarpath中
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
            }

            //测试
            try {
                System.out.println("===================昵称："+Textile.instance().profile.name());
            } catch (Exception e) {
                e.printStackTrace();
            }

            //连网之后反馈给主界面
            EventBus.getDefault().postSticky(Integer.valueOf(0)); //会有先连网后启动ShareFragment的注册，所以用Sticky

        }

        @Override
        public void contactQueryResult(String queryId, Model.Contact contact) {
            EventBus.getDefault().post(contact);
        }

        @Override
        public void threadAdded(String threadId) {
            EventBus.getDefault().post(threadId); //只在添加联系人的时候起作用，创建群组的时候要过滤掉
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

            //如果是不共享的thread，包括相册thread，设备thread，就不对消息进行处理
            if (thread.getSharing().equals(Model.Thread.Sharing.NOT_SHARED)){
                return ;
            }

            boolean isSingle=thread.getWhitelistCount()==2;

            if(feedItemData.type.equals(FeedItemType.REMOVEPEER)){
                //收到自己被移出群组的消息，就要手动删除这个群组
                String removeThreadId=thread.getId();

                //判断是不是删自己，如果是的就把相应的thread和dialog删掉
                String peerId=feedItemData.removePeer.getTarget();
                List<Model.Peer> peers= null;
                try {
                    peers = Textile.instance().threads.peers(threadId).getItemsList();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                for(Model.Peer p:peers){
                    if(p.getAddress().equals(Textile.instance().account.address())){
                        try {
                            Textile.instance().threads.remove(removeThreadId);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        DBoperator.deleteDialogByThreadID(appdb,removeThreadId);
                    }
                }
            }

            if(feedItemData.type.equals(FeedItemType.JOIN)){
                if(DBoperator.queryDialogByThreadID(appdb,threadId)!=null){ //如果已经有了就不要再插入了
                    return;
                }
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
                if(DBoperator.queryDialogByThreadID(appdb,threadId)!=null){ //如果已经有了就不要再插入了
                    return;
                }
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

//                更新dialogs表
                TDialog tDialog=DBoperator.queryDialogByThreadID(appdb,threadId);
                TDialog updateDialog=DBoperator.dialogGetMsg(appdb,tDialog,threadId,
                        feedItemData.text.getBody(), feedItemData.text.getDate().getSeconds(),
                        tDialog.imgpath);

                EventBus.getDefault().post(updateDialog);

                if(ismine==0){ //不是我的消息才广播出去
                    EventBus.getDefault().post(tMsg);
                }
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
                if(ismine==0){  //不是我的图片才广播出去
                    EventBus.getDefault().post(tMsg);
                }
            }
        }
    }

    private void createDeviceThread() {
        //看有没有"mydevice1219"，没有就新建
        try {
            List<Model.Thread> devicethreads=Textile.instance().threads.list().getItemsList();
            boolean hasDevice=false;
            for(Model.Thread t:devicethreads){
                if(t.getName().equals("mydevice1219")){
                    hasDevice=true;
                    System.out.println("=============已经有mydevice1219");
                    break;
                }
            }
            if(!hasDevice){
                String key= UUID.randomUUID().toString();
                sjtu.opennet.textilepb.View.AddThreadConfig.Schema schema= sjtu.opennet.textilepb.View.AddThreadConfig.Schema.newBuilder()
                        .setPreset(View.AddThreadConfig.Schema.Preset.BLOB)
                        .build();
                sjtu.opennet.textilepb.View.AddThreadConfig config=sjtu.opennet.textilepb.View.AddThreadConfig.newBuilder()
                        .setSharing(Model.Thread.Sharing.NOT_SHARED)
                        .setType(Model.Thread.Type.PRIVATE)
                        .setKey(key).setName("mydevice1219")
                        .setSchema(schema)
                        .build();
                Textile.instance().threads.add(config);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
