package com.sjtuopennetwork.shareit.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.util.Pair;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.googlecode.protobuf.format.JsonFormat;
import com.huawei.hms.support.log.LogLevel;
import com.sjtuopennetwork.shareit.R;
import com.sjtuopennetwork.shareit.share.ChatActivity;
import com.sjtuopennetwork.shareit.share.HomeActivity;
import com.sjtuopennetwork.shareit.share.util.PreloadVideoThread;
import com.sjtuopennetwork.shareit.share.util.TDialog;
import com.sjtuopennetwork.shareit.share.util.TMsg;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import sjtu.opennet.hon.BaseTextileEventListener;
import sjtu.opennet.hon.FeedItemData;
import sjtu.opennet.hon.FeedItemType;
import sjtu.opennet.hon.Handlers;
import sjtu.opennet.hon.Logs;
import sjtu.opennet.hon.Textile;
import sjtu.opennet.textilepb.Mobile;
import sjtu.opennet.textilepb.Model;
import sjtu.opennet.textilepb.QueryOuterClass;
import sjtu.opennet.textilepb.View;

public class ForeGroundService extends Service {

    private static final String TAG = "===================";

    private static final String THREAD_UPDATE_LOCK="a";

    public ForeGroundService() {}

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    String loginAccount; //当前登录的账户
    int login;
    String repoPath;
    SharedPreferences pref;
    SQLiteDatabase appdb;
    boolean connectCafe;
    HeartBeat heartBeat;
    boolean startBeat;
    int uploadLogCount=0;
    BlockingQueue<ThreadUpdateEvent> threadUpdateEvents;
    eventCache eventCache;

    class eventCache {
        private ThreadUpdateEvent[] arrayCache;
        private Object LOCK = new Object();
        private int current;
        private int arrayLength;
        eventCache(){
            arrayLength = 30;
            arrayCache = new ThreadUpdateEvent[arrayLength];
            current = 0;
        }

        public boolean HaveEvent(ThreadUpdateEvent tv) {
            synchronized (LOCK) {
                for (int i = current; i < current + arrayLength; i++) {
                    int ind = i % arrayLength;
                    if (arrayCache[ind] != null && tv.feedItemData.block.equals(arrayCache[ind].feedItemData.block)) {
                        return true;
                    }
                }

                arrayCache[current] = tv;
                current = (current + 1) % arrayLength;
                return false;
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        login=intent.getIntExtra("login",0);
        pref=getSharedPreferences("txtl",MODE_PRIVATE);
        repoPath=intent.getStringExtra("repopath");

        connectCafe= pref.getBoolean("connectCafe",false);

        SharedPreferences.Editor editor=pref.edit();
        editor.putBoolean("131ok",false);
        editor.commit();

        new Thread(){
            @Override
            public void run() {
                super.run();
                Log.d(TAG, "run: 启动前台服务");
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

        //消息队列
        threadUpdateEvents=new LinkedBlockingQueue<>();  // 消息队列
        //Message cache
        eventCache = new eventCache();
        new Thread(new threadUpdater()).start();

        return super.onStartCommand(intent, flags, startId);
    }

    public void initTextile(int login){

        String phrase="";
//        final File repoDir= this.getFilesDir();
        final File repoDir = new File(FileUtil.getAppExternalPath(this, "repo"));

        switch (login){
            case 0: //已经登录，找到repo，初始化textile
                loginAccount=pref.getString("loginAccount","null"); //当前登录的account，就是address
                Log.d(TAG, "initTextile: 已经登录过："+loginAccount);
                final File repo0 = new File(repoDir, loginAccount);
                repoPath = repo0.getAbsolutePath();
                Log.d(TAG, "initTextile: repo的路径："+repoPath);
                break;
            case 1: //shareit注册，新建repo，初始化textile
                Log.d(TAG, "initTextile: 注册shareit账号");
                try {
                    phrase= Textile.newWallet(12);
                    Mobile.MobileWalletAccount m=Textile.walletAccountAt(phrase,Textile.WALLET_ACCOUNT_INDEX,Textile.WALLET_PASSPHRASE);
                    loginAccount=m.getAddress(); //获得公钥
                    final File repo1 = new File(repoDir, loginAccount);
                    repoPath = repo1.getAbsolutePath();
                    String pk=m.getSeed(); //获得私钥
                    Textile.initialize(repoPath,pk , true, false, true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case 2: //华为账号登录，找到repo，初始化textile
                Log.d(TAG, "initTextile: 华为ID登录");
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
                    final File repo1 = new File(repoDir, loginAccount);
                    repoPath = repo1.getAbsolutePath();
                    if(!Textile.isInitialized(repoPath)){
                        Textile.initialize(repoPath,m.getSeed() , true, true,true);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case 3: //shareit助记词登录，初始化textile
                Log.d(TAG, "initTextile: 助记词登录");
                phrase=pref.getString("phrase","");
                loginAccount=pref.getString("loginAccount","");
                Log.d(TAG, "initTextile: 助记词："+phrase);
                break;
        }

        //启动Textile
        try {
            Textile.launch(ForeGroundService.this, repoPath, true);
            Textile.instance().addEventListener(new MyTextileListener());
            sjtu.opennet.textilepb.View.LogLevel logLevel= sjtu.opennet.textilepb.View.LogLevel.newBuilder()
                    .putSystems("hon.engine", sjtu.opennet.textilepb.View.LogLevel.Level.DEBUG)
                    .putSystems("hon.bitswap", sjtu.opennet.textilepb.View.LogLevel.Level.DEBUG)
                    .putSystems("hon.peermanager", sjtu.opennet.textilepb.View.LogLevel.Level.DEBUG)
                    .putSystems("tex-core", sjtu.opennet.textilepb.View.LogLevel.Level.DEBUG)
                    .putSystems("tex-mobile", sjtu.opennet.textilepb.View.LogLevel.Level.DEBUG)
//                .putSystems("hon.linkedTicketStorage", sjtu.opennet.textilepb.View.LogLevel.Level.DEBUG)
                    .build();
            Textile.instance().logs.setLevel(logLevel);
            Log.d(TAG, "initTextile: after set log level ");
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
            Log.d(TAG, "initTextile: 即将初始化数据库："+loginAccount);
            appdb= AppdbHelper.getInstance(getApplicationContext(),pref.getString("loginAccount","")).getWritableDatabase();
        }catch (Exception e){
            e.printStackTrace();
        }

        Log.d(TAG, "initTextile: 登录帐户："+loginAccount+" "+phrase);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void tryConnectCafe(Integer register){
        if(register==953){
            Log.d(TAG, "tryConnectCafe: 尝试连接cafe");
            Textile.instance().cafes.register(
//                    "http://159.138.58.61:40601",
                    "http://202.120.38.131:40601",
//                    "http://192.168.1.109:40601",
//                    "http://202.120.40.60:40601"
                    "K6ayNanfZcfvGDBbxmf9DHkeyv4osxoGPpMGNP3vX4DQTnraUugY4h51T6DD", //131
//                    "NhYrQb1XfpCFC7WBhX7UHPkax1o4YvAxxzXhZfLg6qJ5cbbfZakmPQZVer7x",//HW159.138.58.61
//                    "29TkBsmjFfEnR1Sack63qWK5WkPGjJtA2kXFHvTijmSE1KYMvVopBRWagHLbE",
//                    "WwqhHzab1oRqXPs3KnDL2oX1S9h2D7KYotMo2eNUg2MFPJPENWgB1Q2H6m3b",
                    new Handlers.ErrorHandler() {
                        @Override
                        public void onComplete() {
                            Log.d(TAG, "onComplete: 131cafe连接成功");
                            SharedPreferences.Editor editor=pref.edit();
                            editor.putBoolean("131ok",true);
                            editor.putBoolean("connectCafe",true);
                            editor.commit();
                            EventBus.getDefault().post(new Integer(903));

                            QueryOuterClass.QueryOptions options = QueryOuterClass.QueryOptions.newBuilder().build();
                            try {
                                Textile.instance().account.sync(options);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        @Override
                        public void onError(Exception e) {
                            Log.d(TAG, "onError: 131cafe连接失败");
                            //发消息再连接
                            try {
                                Thread.sleep(30000);
                            } catch (InterruptedException ex) {
                                ex.printStackTrace();
                            }
                            EventBus.getDefault().post(new Double(2.34));
                        }
                    });
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void shutDown(Integer stop){
        if(stop==943){
            Log.d(TAG, "shutDown: 服务stop");
            Textile.instance().destroy();
            stopForeground(true);
            stopSelf();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void stopHeartBeat(Integer stopHeart){
        if(stopHeart==933){
            Log.d(TAG, "stopHeartBeat: 停止心跳");
            startBeat=false;
            heartBeat=null;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void startHeartBeat(Integer startHeart){
        if(startHeart==923){
            Log.d(TAG, "startHeartBeat: 开始心跳");
            heartBeat=new HeartBeat();
            startBeat=true;
            heartBeat.start();
        }
    }

    class HeartBeat extends Thread{
        @Override
        public void run() {
            startBeat=true;
            while(startBeat){
                try {
                    Thread.sleep(300000);
                    //发送心跳
                    if(Textile.instance().online()){
                        Model.CafeSessionList sessionList = Textile.instance().cafes.sessions();
                        for (int i = 0; i < sessionList.getItemsCount(); i++) {
                            final Model.CafeSession tmpSession = sessionList.getItems(i);
                            Log.d(TAG, "run: tmpSession: "+tmpSession.getId());
                            Textile.instance().cafes.publishPeerToCafe(tmpSession.getId(), new Handlers.ErrorHandler(){
                                @Override
                                public void onComplete(){
                                    Log.d(TAG, "onComplete: 连接正常");
                                    return;
                                }
                                @Override
                                public void onError(Exception e){
                                    Log.d(TAG, "onError: 连接断开，尝试重连");

                                    Textile.instance().cafes.deregister(tmpSession.getId(), new Handlers.ErrorHandler() {
                                        @Override
                                        public void onComplete() {
                                            tryConnectCafe(new Integer(953));
                                        }

                                        @Override
                                        public void onError(Exception e) {

                                        }
                                    });
                                }
                            });
                        }
                    }else{
                        Log.d(TAG, "run: 节点下线了");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if(EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().unregister(this);
        }
    }

    Handler handler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what){
                case 1:
                    ThreadUpdateEvent tv= null;
                    try {
                        tv = threadUpdateEvents.take();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    String threadId=tv.threadId;
                    FeedItemData feedItemData=tv.feedItemData;
                    handleThreadUpdates(threadId,feedItemData); break;
            }
        }
    };

    class threadUpdater implements Runnable{
        @Override
        public void run(){
            while (true) {
                ThreadUpdateEvent tv = null;
                try {
                    tv = threadUpdateEvents.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    continue;
                }

                if (eventCache.HaveEvent(tv)){
                    Log.d(TAG, "Duplicated block "+ tv.feedItemData.block);
                } else {
                    Log.d(TAG, "New block "+ tv.feedItemData.block);
                    String threadId = tv.threadId;
                    FeedItemData feedItemData = tv.feedItemData;
                    handleThreadUpdates(threadId,feedItemData);
                }
            }
        }
    }

    private void handleThreadUpdates(String threadId, FeedItemData feedItemData) {

        String myAddr=Textile.instance().account.address();

        Model.Thread thread=null;
        try {
            thread=Textile.instance().threads.get(threadId);
            //如果是不共享的thread，包括相册thread，设备thread等，就不对消息进行处理
        } catch (Exception e) {
            e.printStackTrace();
        }
        if ((thread != null) && thread.getSharing().equals(Model.Thread.Sharing.NOT_SHARED)){
            return ;
        }

        if(feedItemData.type.equals(FeedItemType.JOIN)){ //收到JION类型的消息
            if(DBoperator.queryDialogByThreadID(appdb,threadId)!=null){ //如果已经有了就不要再插入了
                return;
            }
            int whiteListCount=thread.getWhitelistCount();
            boolean authorIsMe=feedItemData.join.getUser().getAddress().equals(myAddr); //表明是否是自己的JOIN
            if(whiteListCount==2){ //双人thread
                if(!authorIsMe){  //双人thread收到他人的JOIN，只可能是同意他人好友申请或者自己的好友申请被他人同意，都要插入一条记录
                    TDialog tDialog=DBoperator.insertDialog(appdb,threadId, feedItemData.join.getUser().getName(),
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
                TDialog tDialog=DBoperator.queryDialogByThreadID(appdb,threadId);
                TDialog updateDialog=null;
                if(tDialog!=null){ //如果数据库已经有了，就更新
                    updateDialog=DBoperator.dialogGetMsg(appdb,tDialog,threadId,
                            feedItemData.join.getUser().getName()+" 加入了群组", feedItemData.join.getDate().getSeconds(),
                            tDialog.imgpath);
                }else{ //如果数据库没有就插入
                    System.out.println("================得到群组JOIN消息，创建群组"+feedItemData.join.getUser()+" 加入了群组");
                    updateDialog=DBoperator.insertDialog(appdb,threadId, thread.getName(),
                            feedItemData.join.getUser().getName()+" 加入了群组",
                            feedItemData.join.getDate().getSeconds(),
                            0,
                            feedItemData.join.getUser().getAvatar(),
                            0, 1);
                }
                EventBus.getDefault().post(updateDialog);

                int ismine=0;
                if(feedItemData.join.getUser().getAddress().equals(Textile.instance().account.address())){
                    ismine=1;
                }
                TMsg tMsg=DBoperator.insertMsg(appdb,threadId,0, feedItemData.join.getBlock(),
                        feedItemData.join.getUser().getName(),
                        feedItemData.join.getUser().getAvatar(),
                        feedItemData.join.getUser().getName()+" 加入了群组",
                        feedItemData.join.getDate().getSeconds(), ismine);


                EventBus.getDefault().post(tMsg);
            }

            EventBus.getDefault().post(new Pair(2545,threadId));
        }

        if(feedItemData.type.equals(FeedItemType.TEXT)){ //如果是文本消息
            int ismine=0;
            if(feedItemData.text.getUser().getAddress().equals(myAddr)){
                ismine=1;
            }
            //插入msgs表
            TMsg tMsg=DBoperator.insertMsg(appdb,threadId,0, feedItemData.text.getBlock(),
                    feedItemData.text.getUser().getName(),
                    feedItemData.text.getUser().getAvatar(),
                    feedItemData.text.getBody(),
                    feedItemData.text.getDate().getSeconds(), ismine);

            //更新dialogs表
            TDialog tDialog=DBoperator.queryDialogByThreadID(appdb,threadId);
            TDialog updateDialog=DBoperator.dialogGetMsg(appdb,tDialog,threadId,
                    feedItemData.text.getBody(), feedItemData.text.getDate().getSeconds(),
                    tDialog.imgpath);
            tDialog.isRead=false;
            EventBus.getDefault().post(updateDialog);

            if(ismine==0){ //不是我的消息才广播出去
                EventBus.getDefault().post(tMsg);
            }
        }

        if(feedItemData.type.equals(FeedItemType.PICTURE)){
            TDialog tDialog=DBoperator.queryDialogByThreadID(appdb,threadId); //必然能够查出来对话
            boolean isSingle=thread.getWhitelistCount()==2;
            try {
                //图片消息的hash
                final String large_hash=feedItemData.files.getFiles(0).getFile().getHash();
                Textile.instance().files.content(large_hash, new Handlers.DataHandler() {
                    @Override
                    public void onComplete(byte[] data, String media) { //获得图片成功
                        String newPath=FileUtil.storeFile(data,large_hash); //将图片存到本地
                        String dialogimg="";

                        if(isSingle){ //单人的thread,图片就是对方的头像，不改
                            dialogimg=tDialog.imgpath;
                        }else{
                            dialogimg=newPath; //多人群组的对话图片就要更新
                            Log.d(TAG, "onComplete: dialogimg:"+dialogimg);
                        }
                        Log.d(TAG, "onComplete: 获取图片成功"+newPath);

                        //将更新后的对话存到数据库
                        TDialog updateDialog=DBoperator.dialogGetMsg(appdb,tDialog,threadId,
                                feedItemData.files.getUser().getName()+"分享了图片", feedItemData.files.getDate().getSeconds(),
                                dialogimg);
                        updateDialog.isRead=false;

                        //插入msgs表
                        int ismine=0;
                        if(feedItemData.files.getUser().getAddress().equals(myAddr)){
                            ismine=1;
                        }
                        TMsg tMsg=DBoperator.insertMsg(appdb,threadId,1, feedItemData.files.getBlock(),
                                feedItemData.files.getUser().getName(),
                                feedItemData.files.getUser().getAvatar(),
                                newPath,
                                feedItemData.files.getDate().getSeconds(), ismine);
                        EventBus.getDefault().post(updateDialog);
                        if(ismine==0){  //不是我的图片才广播出去
                            EventBus.getDefault().post(tMsg);
                        }
                    }
                    @Override
                    public void onError(Exception e) { //获得图片失败
                        Log.d(TAG, "onComplete: 获取图片失败");
                        //将更新后的对话存到数据库
                        TDialog updateDialog=DBoperator.dialogGetMsg(appdb,tDialog,threadId,
                                feedItemData.files.getUser().getName()+"分享了图片", feedItemData.files.getDate().getSeconds(),
                                "null");
                        updateDialog.isRead=false;

                        //插入msgs表
                        int ismine=0;
                        if(feedItemData.files.getUser().getAddress().equals(myAddr)){
                            ismine=1;
                        }
//                        TMsg tMsg=DBoperator.insertMsg(appdb,threadId,1, feedItemData.files.getBlock(),
//                                feedItemData.files.getUser().getName(),
//                                feedItemData.files.getUser().getAvatar(),
//                                "null",
//                                feedItemData.files.getDate().getSeconds(), ismine);
//                        EventBus.getDefault().post(updateDialog);
//                        if(ismine==0){  //不是我的图片才广播出去
//                            EventBus.getDefault().post(tMsg);
//                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        if(feedItemData.type.equals(FeedItemType.FILES)){ //接收到图片
            Log.d(TAG, "handleThreadUpdates: 收到files");
        }

        if(feedItemData.type.equals(FeedItemType.VIDEO)){
            Model.Video video=feedItemData.feedVideo.getVideo();

            //每得到一个视频就在后台启动预加载线程
//                new PreloadVideoThread(getApplicationContext(),video.getId()).start();

            TDialog tDialog=DBoperator.queryDialogByThreadID(appdb,threadId);
            TDialog updateDialog=DBoperator.dialogGetMsg(appdb,tDialog,threadId,
                    feedItemData.feedVideo.getUser().getName()+"分享了视频", feedItemData.feedVideo.getDate().getSeconds(),
                    tDialog.imgpath);
            Log.d(TAG, "handleThreadUpdates: postDialog消息");
            EventBus.getDefault().post(updateDialog);


            String posterHash=video.getPoster();
            String videoPath=video.getCaption();
            String videoID=video.getId();
            Log.d(TAG, "threadUpdateReceived: 得到视频的地址："+videoPath);
            Textile.instance().ipfs.dataAtPath(posterHash, new Handlers.DataHandler() {
                @Override
                public void onComplete(byte[] data, String media) { //成功拿到缩略图
                    String posterPath=FileUtil.storeFile(data,posterHash); //将图片存到本地
                    String msgBody=posterPath+"##"+videoID+"##"+videoPath;
                    //Msg
                    int ismine=0;
                    if(feedItemData.feedVideo.getUser().getAddress().equals(myAddr)){
                        ismine=1;
                    }
                    //插入msgs表
                    TMsg tMsg=DBoperator.insertMsg(appdb,threadId,2, feedItemData.feedVideo.getBlock(),
                            feedItemData.feedVideo.getUser().getName(),
                            feedItemData.feedVideo.getUser().getAvatar(),
                            msgBody, //poster和id的hash值
                            feedItemData.feedVideo.getDate().getSeconds(), ismine);
                    if(ismine==0){  //不是我的视频才广播出去，因为我自己的消息直接显示了
                        Log.d(TAG, "onComplete: postMsg消息");
                        EventBus.getDefault().post(tMsg);
                    }
                }

                @Override
                public void onError(Exception e) {
                    String msgBody="null##"+videoID+"##"+videoPath;
                    //Msg
                    int ismine=0;
                    if(feedItemData.feedVideo.getUser().getAddress().equals(myAddr)){
                        ismine=1;
                    }
                    //插入msgs表
                    TMsg tMsg=DBoperator.insertMsg(appdb,threadId,2, feedItemData.feedVideo.getBlock(),
                            feedItemData.feedVideo.getUser().getName(),
                            feedItemData.feedVideo.getUser().getAvatar(),
                            msgBody, //poster和id的hash值
                            feedItemData.feedVideo.getDate().getSeconds(), ismine);
                    if(ismine==0){  //不是我的视频才广播出去，因为我自己的消息直接显示了
                        EventBus.getDefault().post(tMsg);
                    }
                }
            });
        }

        if(feedItemData.type.equals(FeedItemType.STREAMMETA)){ //得到stream

            Log.d(TAG, "handleThreadUpdates: =====收到stream");

            int ismine=0;
            if(feedItemData.feedStreamMeta.getUser().getAddress().equals(myAddr)){
                ismine=1;
            }
            if(ismine==0){  //不是我的视频才广播出去，因为我自己的消息直接显示了
                String msgBody=feedItemData.feedStreamMeta.getStreammeta().getId();
                TMsg tMsg=DBoperator.insertMsg(appdb,threadId,2,feedItemData.feedStreamMeta.getBlock(),
                        feedItemData.feedStreamMeta.getUser().getName(),
                        feedItemData.feedStreamMeta.getUser().getAvatar(),
                        msgBody,
                        feedItemData.feedStreamMeta.getDate().getSeconds(), ismine);

                Log.d(TAG, "onComplete: postMsg消息");
                EventBus.getDefault().post(tMsg);
            }
        }

        if(feedItemData.type.equals(FeedItemType.REMOVEPEER)){
            //收到自己被移出群组的消息，就要手动删除这个群组
            String removeThreadId=thread.getId();

            //判断是不是删自己，如果是的就把相应的thread和dialog删掉
            String peerId=feedItemData.removePeer.getTarget();
            Model.User user = null;
            try {
                user = Textile.instance().peers.peerUser(peerId);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if(user.getAddress().equals(Textile.instance().account.address())){
                try {
                    Textile.instance().threads.remove(removeThreadId);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                DBoperator.deleteDialogByThreadID(appdb,removeThreadId);
            }
        }
    }

    class MyTextileListener extends BaseTextileEventListener {

        @Override
        public void notificationReceived(Model.Notification notification) {


            if(notification.getBody().equals("stream file")){
                Log.d(TAG, "notificationReceived: 收到streamfile");
                try {
                    View.VideoDescription.Builder b=View.VideoDescription.newBuilder();
                    JsonFormat.merge(notification.getSubjectDesc(),b);
                    View.VideoDescription videoDescription= b.build();
                    Log.d(TAG, "notificationReceived: "+ videoDescription);
                    EventBus.getDefault().post(new Pair<>(notification.getBlock(),videoDescription));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }else{
                //查出邀请中最近的一个，添加到头部。
                int gpinvite = 0;
                sjtu.opennet.textilepb.View.InviteView lastInvite = null;
                try {
                    if (Textile.instance().invites != null) {
                        List<sjtu.opennet.textilepb.View.InviteView> invites = Textile.instance().invites.list().getItemsList();
                        for (sjtu.opennet.textilepb.View.InviteView v : invites) { //遍历所有的邀请
                            if (!v.getName().equals("FriendThread1219")) { //只要群组名不等于这个那就是好友邀请
                                gpinvite++;
                                lastInvite = v;
                            }
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (gpinvite > 0) { //如果有群组邀请就要显示出来
                    TDialog noti = new TDialog(1, "", "通知", lastInvite.getInviter().getName() + " 邀请你",
                            lastInvite.getDate().getSeconds(), false, "tongzhi", true, true);

                    EventBus.getDefault().post(noti);
                }
            }
        }

        @Override
        public void nodeOnline() {

//            根据登录方式，设置name和头像
            Log.d(TAG, "nodeOnline: login的值："+login);
            switch(login){
                case 0: //已经登录过的，不用设置name、avatar
                    break;
                case 1: //shareit注册，每次都要设置
                    String shareitName=pref.getString("myname","");
                    String shareitAvatarpath=pref.getString("avatarpath","");
                    Log.d(TAG, "nodeOnline: shareIt注册的name和avatar："+shareitName+" "+shareitAvatarpath);
                    try {
                        Textile.instance().profile.setName(shareitName);
                        if(!shareitAvatarpath.equals("")){
                            Textile.instance().profile.setAvatar(shareitAvatarpath, new Handlers.BlockHandler() {
                                @Override
                                public void onComplete(Model.Block block) {
                                    Log.d(TAG, "onComplete: Shareit注册设置头像成功");
                                    QueryOuterClass.QueryOptions options = QueryOuterClass.QueryOptions.newBuilder().build();
                                    try {
                                        Textile.instance().account.sync(options);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }

                                @Override
                                public void onError(Exception e) {
                                    Log.d(TAG, "onError: ShareIt注册设置头像失败");
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
                        }
                        String huaweiAvatarpath=pref.getString("avatarpath","");
                        if(!Textile.instance().profile.avatar().equals(huaweiAvatarpath)){
                            if(!huaweiAvatarpath.equals("")){ //后台线程已经拿到头像
                                Textile.instance().profile.setAvatar(huaweiAvatarpath, new Handlers.BlockHandler() {
                                    @Override
                                    public void onComplete(Model.Block block) {
                                        Log.d(TAG, "onComplete: 华为Id登录设置头像成功");
                                    }

                                    @Override
                                    public void onError(Exception e) {
                                        Log.d(TAG, "onComplete: 华为Id登录设置头像失败");
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
                            Log.d(TAG, "nodeOnline: 助记词登录，未同步name");
                        }else{
                            //put到myname中
                        }
                        if(Textile.instance().profile.avatar().equals("")) { //如果没有同步过来，
                            Log.d(TAG, "nodeOnline: 助记词登录，未同步avatar");
                        }else{
                            //获得头像，并将路径put到avatarpath中
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
            }

            QueryOuterClass.QueryOptions options = QueryOuterClass.QueryOptions.newBuilder().build();
            try {
                Textile.instance().account.sync(options);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if(connectCafe){
                tryConnectCafe(new Integer(953));

                startHeartBeat(new Integer(923));
            }

            createDeviceThread();

            //QmQJf71bndD1p7i2h2sYvWEkhz8SD1vpLp8LZ6eBeTWSER   21wAacMe62Q1fVKth69GLmwj68yRYbYAVjQ9im2DT72d6BAGpDYpn6nMEMR1W
//            addIntoWorldThread();

            //测试name
            try {
                Log.d(TAG, "nodeOnline: peerID: "+Textile.instance().profile.get().getId());
                Log.d(TAG, "nodeOnline: 账户name："+Textile.instance().profile.name());
            } catch (Exception e) {
                e.printStackTrace();
            }

            //连网之后反馈给主界面
            EventBus.getDefault().post(Integer.valueOf(0));
        }

        private void addIntoWorldThread() {
            try {
                Textile.instance().invites.acceptExternal("QmQJf71bndD1p7i2h2sYvWEkhz8SD1vpLp8LZ6eBeTWSER","21wAacMe62Q1fVKth69GLmwj68yRYbYAVjQ9im2DT72d6BAGpDYpn6nMEMR1W");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void contactQueryResult(String queryId, Model.Contact contact) {
            EventBus.getDefault().post(contact);
        }

        @Override
        public void threadAdded(String threadId) {
            Log.d(TAG, "threadAdded: 创建了一个thread");
            EventBus.getDefault().post(threadId); //只在添加联系人的时候起作用，创建群组的时候要过滤掉
        }

        @Override
        public void videoChunkQueryResult(String queryId, Model.VideoChunk vchunk) {
            Log.d(TAG, "videoChunkQueryResult: VIDEOCHUNK得到结果："+vchunk.getIndex());
            EventBus.getDefault().post(vchunk);
        }

        @Override
        public void syncFileQueryResult(final String queryId, final Model.SyncFile file){
            Log.d(TAG, String.format("Get sync file query result %s:%s",file.getPeerAddress(), file.getFile()));
            EventBus.getDefault().post(file);
        }

        @Override
        public void threadUpdateReceived(String threadId, FeedItemData feedItemData) {

                try {

//                    if(DBoperator.isMsgExist(appdb,feedItemData.block)){
//                        Log.d(TAG, "threadUpdateReceived: 已经存在："+feedItemData.block);
//                        return;
//                    }
//
//                    for(ThreadUpdateEvent t:threadUpdateEvents){
//                        if(t.feedItemData.block.equals(feedItemData.block)){
//                            Log.d(TAG, "threadUpdateReceived: 收到重复消息："+t.feedItemData.block);
//                            return;
//                        }
//                    }

                    threadUpdateEvents.put(new ThreadUpdateEvent(threadId,feedItemData));
                    Log.d(TAG, "threadUpdateReceived: 消息添加到队列："+feedItemData.type.name()+" "+feedItemData.block);
//
//                    Message msg=new Message();
//                    msg.what=1;
//                    handler.sendMessage(msg);
                } catch (Exception e) {
                    e.printStackTrace();
                }

//                handleThreadUpdates(threadId,feedItemData);
        }

        private void createDeviceThread() {
            //看有没有"mydevice1219"，没有就新建
            try {
                List<Model.Thread> devicethreads=Textile.instance().threads.list().getItemsList();
                boolean hasDevice=false;
                boolean hasStorage=false;
                for(Model.Thread t:devicethreads){
                    if(t.getName().equals("mydevice1219")){
                        hasDevice=true;
                        break;
                    }
                    if(t.getName().equals("!@#$1234FileStorage")){
                        hasStorage=true;
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
                if(!hasStorage){
                    String key= UUID.randomUUID().toString();
                    sjtu.opennet.textilepb.View.AddThreadConfig.Schema schema= sjtu.opennet.textilepb.View.AddThreadConfig.Schema.newBuilder()
                            .setPreset(View.AddThreadConfig.Schema.Preset.BLOB)
                            .build();
                    sjtu.opennet.textilepb.View.AddThreadConfig config=sjtu.opennet.textilepb.View.AddThreadConfig.newBuilder()
                            .setSharing(Model.Thread.Sharing.NOT_SHARED)
                            .setType(Model.Thread.Type.PRIVATE)
                            .setKey(key).setName("!@#$1234FileStorage")
                            .setSchema(schema)
                            .build();
                    Textile.instance().threads.add(config);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
