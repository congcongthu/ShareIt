package com.sjtuopennetwork.shareit.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.util.Pair;

import com.googlecode.protobuf.format.JsonFormat;
import com.sjtuopennetwork.shareit.R;
import com.sjtuopennetwork.shareit.share.util.TDialog;
import com.sjtuopennetwork.shareit.share.util.TMsg;
import com.sjtuopennetwork.shareit.share.util.TRecord;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import sjtu.opennet.hon.BaseTextileEventListener;
import sjtu.opennet.hon.FeedItemData;
import sjtu.opennet.hon.FeedItemType;
import sjtu.opennet.hon.Handlers;
import sjtu.opennet.hon.Textile;
import sjtu.opennet.textilepb.Mobile;
import sjtu.opennet.textilepb.Model;
import sjtu.opennet.textilepb.QueryOuterClass;
import sjtu.opennet.textilepb.RecordService;
import sjtu.opennet.textilepb.View;
import com.sjtuopennetwork.shareit.Constant;
public class ShareService extends Service {
    public ShareService() {
    }
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private static final String TAG = "========ShareService";

    private String loginAccount;
    private int login;
    private String repoPath;
    private SharedPreferences pref;
    private boolean connectCafe;
    private String myname;
    private String avatarpath;
    private String lastBlock="0";
    private boolean serviceOn=true;
    private final Object LOCK=new Object();
    private HashMap<String, LinkedList<FileTransInfo>> recordTmp=new HashMap<>();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        login=intent.getIntExtra("login",0);
        myname=intent.getStringExtra("myname");
        avatarpath=intent.getStringExtra("avatarpath");
        repoPath=intent.getStringExtra("repopath");

        pref=getSharedPreferences("txtl",MODE_PRIVATE);
        connectCafe= pref.getBoolean("connectCafe",true);
//        connectCafe= pref.getBoolean("connectCafe",true);

        new Thread(){
            @Override
            public void run() {
                super.run();
                Log.d(TAG, "run: 启动前台服务");
                NotificationManager notificationManager=(NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                NotificationChannel notificationChannel=new NotificationChannel("12","前台服务",NotificationManager.IMPORTANCE_HIGH);
                notificationManager.createNotificationChannel(notificationChannel);
                Notification notification=new Notification.Builder(ShareService.this,"12")
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

    public void initTextile(int login) {
        final File repoDir = new File(ShareUtil.getAppExternalPath(this, "repo"));
        String phrase="";
        //初始化repo
        switch (login) {
            case Constant.LOGINED: //已经登录，找到repo，初始化textile
                loginAccount=pref.getString("loginAccount","null"); //当前登录的account，就是address
                final File repo0 = new File(repoDir, loginAccount);
                repoPath = repo0.getAbsolutePath();
                break;
            case Constant.SHAREIT_LOGIN: //shareit注册，新建repo，初始化textile
                try {
                    phrase= Textile.newWallet(12); //助记词
                    Mobile.MobileWalletAccount m=Textile.walletAccountAt(phrase,Textile.WALLET_ACCOUNT_INDEX,Textile.WALLET_PASSPHRASE);
                    loginAccount=m.getAddress(); //获得公钥
                    final File repo1 = new File(repoDir, loginAccount);
                    repoPath = repo1.getAbsolutePath();
                    String sk=m.getSeed(); //获得私钥
                    Textile.initialize(repoPath,sk , true, false, true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case Constant.HUAWEI_LOGIN: //华为账号登录，找到repo，初始化textile
                String openid=pref.getString("openid",""); //?测试一下是否需要截断，应该并不需要
                String avatarUri=pref.getString("avatarUri",""); //先判断一下是否已经存储过
                new Thread(){
                    @Override
                    public void run() {
                        avatarpath = ShareUtil.getHuaweiAvatar(avatarUri);
                    }
                }.start();
                try {
                    phrase=Textile.newWalletFromHuaweiOpenId(openid);
                    Mobile.MobileWalletAccount m=Textile.walletAccountAt(phrase,Textile.WALLET_ACCOUNT_INDEX,Textile.WALLET_PASSPHRASE);
                    loginAccount=m.getAddress();
                    final File repo1 = new File(repoDir, loginAccount);
                    repoPath = repo1.getAbsolutePath();
                    if(!Textile.isInitialized(repoPath)){
                        Textile.initialize(repoPath,m.getSeed() , true, false,true);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case Constant.LOGINED_AND_INITED: //shareit助记词登录,已经初始化了，只需要设置一些变量
                phrase=pref.getString("phrase","");
                loginAccount=pref.getString("loginAccount","");
                break;

        }

        //启动Textile节点
        try {
            Textile.launch(ShareService.this, repoPath, true);
            Textile.instance().addEventListener(new ShareListener());
            sjtu.opennet.textilepb.View.LogLevel logLevel= sjtu.opennet.textilepb.View.LogLevel.newBuilder()
                    .putSystems("hon.engine", sjtu.opennet.textilepb.View.LogLevel.Level.DEBUG)
                    .putSystems("hon.bitswap", sjtu.opennet.textilepb.View.LogLevel.Level.DEBUG)
                    .putSystems("hon.peermanager", sjtu.opennet.textilepb.View.LogLevel.Level.DEBUG)
                    .putSystems("tex-core", sjtu.opennet.textilepb.View.LogLevel.Level.DEBUG)
                    .putSystems("tex-mobile", sjtu.opennet.textilepb.View.LogLevel.Level.DEBUG)
                    .putSystems("tex-service",sjtu.opennet.textilepb.View.LogLevel.Level.DEBUG)
                    .build();
            Textile.instance().logs.setLevel(logLevel);
        } catch (Exception e) {
            e.printStackTrace();
        }

        SharedPreferences.Editor editor=pref.edit();
        editor.putBoolean("isLogin",true);
        if(login==Constant.SHAREIT_LOGIN || login ==Constant.HUAWEI_LOGIN){ //1,2都需要修改助记词和登录账户,3不需要
            editor.putString("phrase",phrase);
            editor.putString("loginAccount",loginAccount);
        }
        editor.commit();
    }

    class ThreadUpdateWork{
        Model.Thread thread;
        FeedItemData feedItemData;
        public ThreadUpdateWork(Model.Thread t, FeedItemData f){
            thread=t;
            feedItemData=f;
        }
    }

    Handler threadUpdateHandler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            ThreadUpdateWork threadUpdateWork=(ThreadUpdateWork) msg.obj;
            handleThreadUpdate(threadUpdateWork);
        }
    };

    public void handleThreadUpdate(ThreadUpdateWork threadUpdateWork){
        Log.d(TAG, "处理消息："+threadUpdateWork.feedItemData.type+" "+threadUpdateWork.feedItemData.block);
        Model.Thread thread=threadUpdateWork.thread;
        FeedItemData feedItemData=threadUpdateWork.feedItemData;

        String myAddr=Textile.instance().account.address();
        String threadId=thread.getId();

        TDialog tDialog=DBHelper.getInstance(getApplicationContext(),loginAccount).queryDialogByThreadID(threadId); //必然能够查出来对话

        if(feedItemData.type.equals(FeedItemType.JOIN)){ //如何根据JOIN类消息创建对话？
            if(tDialog!=null){ //如果已经有了就不要再插入了
                return; //如数据库已经记录了这个对话，为了简化逻辑，就不再加入
            }
            //如果首次收到这个thread的join，无论是对方还是我方，还是群组，都直接加入。
            int whiteListCount=thread.getWhitelistCount();
            boolean authorIsMe=feedItemData.join.getUser().getAddress().equals(myAddr); //表明是否是自己的JOIN
            boolean flag=false; //我的

            int isSingle=0;
            String add_or_img="";
            if(whiteListCount == 2){
                if (!authorIsMe){//双人，不是我，则是他人的好友同意
                    isSingle=1;
                    add_or_img=feedItemData.join.getUser().getAddress();
                    Log.d(TAG, "threadUpdateReceived: get friend agree: "+add_or_img);
                    flag=true;
                }
            }else{ //群组
                isSingle=0;
                flag=true;
                Log.d(TAG, "threadUpdateReceived: get group");
            }

            if(flag){ //群组，或者双人接收才创建
                TDialog newDialog=DBHelper.getInstance(getApplicationContext(),loginAccount).insertDialog(
                        threadId,
                        "你好啊，现在我们已经成为好友了",
                        feedItemData.join.getDate().getSeconds(),
                        0, //后台收到默认是未读的
                        add_or_img,
                        isSingle, 1);
                EventBus.getDefault().post(newDialog);
            }
        }

        if(feedItemData.type.equals(FeedItemType.TEXT)){ //如果是文本消息
            int ismine=0;
            if(feedItemData.text.getUser().getAddress().equals(myAddr)){ // 是我自己的消息
                if(feedItemData.text.getBody().equals("ack")){ //自己的ack直接忽略
                    return;
                }
                ismine=1;
            }else{ // 别人的消息
                if(feedItemData.text.getBody().equals("ack")){
//                    EventBus.getDefault().post(new Long(System.currentTimeMillis()));
                    Log.d(TAG, "handleThreadUpdate: get other's ack");
                    return;
                } // 回复别人的消息
                try {
                    Textile.instance().messages.add(threadId,"ack");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            //插入msgs表
            TMsg tMsg=DBHelper.getInstance(getApplicationContext(),loginAccount).insertMsg(
                    threadId,0, feedItemData.text.getBlock(),
                    feedItemData.text.getUser().getAddress(),
                    feedItemData.text.getBody(),
                    feedItemData.text.getDate().getSeconds(), ismine);

            //更新dialogs表
            TDialog updateDialog=DBHelper.getInstance(getApplicationContext(),loginAccount).dialogGetMsg(tDialog,threadId,
                    feedItemData.text.getBody(), feedItemData.text.getDate().getSeconds(),
                    tDialog.add_or_img);
            tDialog.isRead=false;

            EventBus.getDefault().post(updateDialog);
            EventBus.getDefault().post(tMsg); //我的消息也要广播，所有的消息的显示都不要从本地来，而是后发送，本地其实还是很快的。

        }

        if(feedItemData.type.equals(FeedItemType.PICTURE)){
            boolean isSingle=thread.getWhitelistCount()==2;
            final String hash=feedItemData.files.getFiles(0).getFile().getHash(); //可取数据的ipfs路径
            String fileName=feedItemData.files.getFiles(0).getFile().getName();
            String body=hash+"##"+fileName;
            String dialogimg="";
            if(isSingle){ //单人的thread,图片就是对方的头像，不改
                dialogimg=tDialog.add_or_img;
            }else{ //多人的要更新
                dialogimg=hash;
            }
            TDialog updateDialog=DBHelper.getInstance(getApplicationContext(),loginAccount).dialogGetMsg(tDialog,threadId,
                    feedItemData.files.getUser().getName()+"分享了图片", feedItemData.files.getDate().getSeconds(),
                    dialogimg);
            updateDialog.isRead=false;
            //插入msgs表
            int ismine=0;
            if(feedItemData.files.getUser().getAddress().equals(myAddr)){
                ismine=1;
            }
            TMsg tMsg=DBHelper.getInstance(getApplicationContext(),loginAccount).insertMsg(
                    threadId,1, feedItemData.files.getBlock(),
                    feedItemData.files.getUser().getAddress(),
                    body,
                    feedItemData.files.getDate().getSeconds(), ismine);
            EventBus.getDefault().post(updateDialog);
            EventBus.getDefault().post(tMsg);
        }

        if(feedItemData.type.equals(FeedItemType.FILES)){
            String fileHash=feedItemData.files.getFiles(0).getFile().getHash();
            String fileName=feedItemData.files.getFiles(0).getFile().getName();
            Log.d(TAG, "handleThreadUpdate: 1:"+feedItemData.block);
            Log.d(TAG, "handleThreadUpdate: 2: "+feedItemData.files.getBlock());
            int ismine=0;
            if(feedItemData.files.getUser().getAddress().equals(myAddr)){
                ismine=1;
            }else{
                Textile.instance().files.content(fileHash, new Handlers.DataHandler() {
                    @Override
                    public void onComplete(byte[] data, String media) {
                        ShareUtil.storeSyncFile(data,fileName);
                    }
                    @Override
                    public void onError(Exception e) {
                    }
                });
            }
            Log.d(TAG, "handleThreadUpdate: fileHash: "+fileHash);
            String body=fileHash+"##"+fileName+"##"+feedItemData.block;
            TMsg tMsg=DBHelper.getInstance(getApplicationContext(),loginAccount).insertMsg(
                    threadId,3,feedItemData.files.getBlock(),
                    feedItemData.files.getUser().getAddress(),
                    body,
                    feedItemData.files.getDate().getSeconds(),ismine);
            TDialog updateDialog=DBHelper.getInstance(getApplicationContext(),loginAccount).dialogGetMsg(
                    tDialog,threadId,feedItemData.files.getUser().getName()+"分享了文件",
                    feedItemData.files.getDate().getSeconds(),tDialog.add_or_img);
            EventBus.getDefault().post(updateDialog);
            EventBus.getDefault().post(tMsg);
        }

        if(feedItemData.type.equals(FeedItemType.STREAMMETA)){ //得到stream
            Log.d(TAG, "handleThreadUpdates: =====收到stream");
            int ismine=0;
            if(feedItemData.feedStreamMeta.getUser().getAddress().equals(myAddr)){
                ismine=1;
            }
            if(ismine==0){ //
                String streamId=feedItemData.feedStreamMeta.getStreammeta().getId();
                String posterId=feedItemData.feedStreamMeta.getStreammeta().getPosterid();
                String body=posterId+"##"+streamId;
                TMsg tMsg=DBHelper.getInstance(getApplicationContext(),loginAccount).insertMsg( // stream视频就是将缩略图hash和streamid放入消息，并设置消息的类型为2
                        threadId,2,feedItemData.feedStreamMeta.getBlock(),
                        feedItemData.feedStreamMeta.getUser().getAddress(),body,
                        feedItemData.feedStreamMeta.getDate().getSeconds(),ismine);
                Log.d(TAG, "onComplete: postMsg消息");
                EventBus.getDefault().post(tMsg);
            }
        }

        if(feedItemData.type.equals(FeedItemType.VIDEO)){
            Model.Video video=feedItemData.feedVideo.getVideo();
            int ismine=0;
            if(feedItemData.feedVideo.getUser().getAddress().equals(myAddr)){
                ismine=1;
            }

            TDialog updateDialog=DBHelper.getInstance(getApplicationContext(),loginAccount).dialogGetMsg(
                    tDialog,threadId,feedItemData.feedVideo.getUser().getName()+"分享了视频",
                    feedItemData.feedVideo.getDate().getSeconds(),tDialog.add_or_img);
            EventBus.getDefault().post(updateDialog);

            if(ismine==0){
                String posterHash=video.getPoster();
                String videoId=video.getId();
                String body=posterHash+"##"+videoId;
                Log.d(TAG, "threadUpdateReceived: getVideo: "+videoId+" "+posterHash);
                TMsg tMsg=DBHelper.getInstance(getApplicationContext(),loginAccount).insertMsg( // ticket类型的视频，就将缩略图hash和videoid放入，设置消息类型为4
                        threadId,4,feedItemData.feedVideo.getBlock(),
                        feedItemData.feedVideo.getUser().getAddress(),body,
                        feedItemData.feedVideo.getDate().getSeconds(),ismine);
                EventBus.getDefault().post(tMsg);
            }
        }

        if(feedItemData.type.equals(FeedItemType.SIMPLEFILE)){
            String fileHash=feedItemData.feedSimpleFile.getSimpleFile().getPath();
            String fileName=feedItemData.feedSimpleFile.getSimpleFile().getName();
            Log.d(TAG, "handleThreadUpdate: simple: "+fileHash);
            int ismine=0;
            if(feedItemData.feedSimpleFile.getUser().getAddress().equals(myAddr)){
                ismine=1;
            }
            int simpleType=0;
            if(feedItemData.feedSimpleFile.getSimpleFile().getType().equals(Model.SimpleFile.Type.PICTURE)) {
                simpleType = 6;
            }else{
                simpleType=5;
            }
            String body=fileHash+"##"+fileName+"##"+feedItemData.block;
            TMsg tMsg=DBHelper.getInstance(getApplicationContext(),loginAccount).insertMsg(
                    threadId,simpleType,feedItemData.feedSimpleFile.getBlock(),
                    feedItemData.feedSimpleFile.getUser().getAddress(),
                    body,
                    feedItemData.feedSimpleFile.getDate().getSeconds(),ismine);

            if(feedItemData.feedSimpleFile.getSimpleFile().getType().equals(Model.SimpleFile.Type.PICTURE)){
                Textile.instance().ipfs.dataAtFeedSimpleFile(feedItemData.feedSimpleFile, new Handlers.DataHandler() {
                    @Override
                    public void onComplete(byte[] data, String media) {
                        String a=ShareUtil.cacheImg(data,fileHash);
                        Log.d(TAG, "onComplete: filesize:"+data.length+" "+a);
                        EventBus.getDefault().post(tMsg);
                    }

                    @Override
                    public void onError(Exception e) {

                    }
                });

            }else{
                Textile.instance().ipfs.dataAtFeedSimpleFile(feedItemData.feedSimpleFile, new Handlers.DataHandler() {
                    @Override
                    public void onComplete(byte[] data, String media) {
                        ShareUtil.storeSyncFile(data, fileName);
                        Log.d(TAG, "onComplete: filesize:"+data.length);
                    }

                    @Override
                    public void onError(Exception e) {

                    }
                });
                EventBus.getDefault().post(tMsg);
            }
        }

    }

    class FileTransInfo{
        public String peerkey;
        public long gettime;
        public FileTransInfo(String a, long b){
            peerkey=a;
            gettime=b;
        }
    }

    class ShareListener extends BaseTextileEventListener {

        @Override
        public void nodeOnline() {
            super.nodeOnline();

            if(login == 1 || login==2){ // 0直接进来不用设置，1/2新登录需要设置，3新登录但是没有用户名不用设置
                try {
                    Textile.instance().profile.setName(myname);
                    if(avatarpath!=null){
                        Textile.instance().profile.setAvatar(avatarpath, new Handlers.BlockHandler() {
                            @Override
                            public void onComplete(Model.Block block) {
                                Log.d(TAG, "onComplete: Shareit注册设置头像成功");
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
            }

            try {
                boolean s1=Textile.instance().ipfs.swarmConnect("/ip6/2001:da8:8000:6084:1a31:bfff:fecf:e603/tcp/4001/ipfs/12D3KooWFHnbHXpDyW1nQxdjJ6ETauAfunj3g2ZtRU4xV9AkZxCq");
                boolean s2=Textile.instance().ipfs.swarmConnect("/ip4/202.120.38.131/tcp/4001/ipfs/12D3KooWKAKVbQF5yUGAaE5uDnuEbZAAgeU6cUYME6FhqFvqWmay");
                boolean s3=Textile.instance().ipfs.swarmConnect("/ip4/202.120.38.100/tcp/4001/ipfs/12D3KooWHp3ABxB1E4ebeEpvcViVFUSsaH198QopBQ8pygvF6PzX");
                Log.d(TAG, "nodeOnline: swarmConnect: "+s1+" "+s2+" "+s3);
            } catch (Exception e) {
                e.printStackTrace();
            }

            //connect cafe
            Log.d(TAG, "nodeOnline: connectcafe:"+connectCafe);
            if(connectCafe){
                CafeUtil.connectCafe(new Handlers.ErrorHandler() {
                    @Override
                    public void onComplete() {
                        Log.d(TAG, "onComplete: cafe连接成功1");
                        SharedPreferences.Editor editor=pref.edit();
                        editor.putBoolean("ok131",true);
                        editor.putBoolean("connectCafe",true);
                        editor.commit();
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.d(TAG, "onError: cafe连接失败1");
                        SharedPreferences.Editor editor=pref.edit();
                        editor.putBoolean("ok131",false);
                        editor.commit();
                    }
                });
            }

            ShareUtil.createDeviceThread();

            // join the default thread after online, the thread is created by cafe
            if(ShareUtil.getThreadByName("default")==null){
                try {
                    Textile.instance().invites.acceptExternal("QmdocmhxFuJ6SdGMT3Arh5wacWnWjZ52VsGXdSp6aXhTVJ","2NfdMrvABwHorxeJxSckSkBKfBJMMF4LqGwdmjY5ZCKw8TDpfHYxELbWnNhed");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            //
            EventBus.getDefault().post(Integer.valueOf(0));
        }

        @Override
        public void notificationReceived(Model.Notification notification) {
            Log.d(TAG, "notificationReceived, type: "+notification.getType()+" "+notification.getSubject());
            if( notification.getType().equals(Model.Notification.Type.RECORD_REPORT)){
                if(notification.getUser().getAddress().equals(Textile.instance().account.address())){
                    Log.d(TAG, "notificationReceived: 自己的notification");
                    return;
                }
                if(notification.getSubject().equals("ipfsGet")){
                    LinkedList<FileTransInfo> l=null;
                    if(!recordTmp.containsKey(notification.getBlock())){
                        l=new LinkedList<>();
                    }else{
                        l=recordTmp.get(notification.getBlock());
                    }
                    long gett1=notification.getDate().getSeconds()*1000+(notification.getDate().getNanos()/1000000);
                    FileTransInfo fileTransInfo=new FileTransInfo(notification.getActor(),gett1);
                    l.add(fileTransInfo);
                    Log.d(TAG, "notificationReceived: ipfsGet,sec,nanosec: "+gett1+" "+notification.getBlock());
                    recordTmp.put(notification.getBlock(),l);
                    Log.d(TAG, "notificationReceived: l size:"+l.size());
                }else if(notification.getSubject().equals("ipfsDone")){
                    LinkedList<FileTransInfo> l=recordTmp.get(notification.getBlock());
                    Log.d(TAG, "notificationReceived: block: "+notification.getBlock()+" "+l.size());
                    int i=0;
                    TRecord tRecord=null;
                    for(;i<l.size();i++){
                        if(l.get(i).peerkey.equals(notification.getActor())){ //找到那个人的get1
                            long get1=l.get(i).gettime;
                            Log.d(TAG, "notificationReceived: get1: "+get1+" "+i);
                            long get2=notification.getDate().getSeconds()*1000+(notification.getDate().getNanos()/1000000);
                            Log.d(TAG, "notificationReceived: ipfsDone,sec,nanosec: "+get2);
                            tRecord=new TRecord(notification.getBlock(),notification.getActor(),get1,get2,System.currentTimeMillis(),1);
                            DBHelper.getInstance(getApplicationContext(),loginAccount).recordGet(tRecord.cid,tRecord.recordFrom,Long.valueOf(get1),get2,tRecord.t3);
                            Log.d(TAG, "notificationReceived: cid, get1, get2: "+tRecord.cid+" "+get1+" "+get2);
                            break;
                        }
                    }
                    l.remove(i);
                    EventBus.getDefault().post(tRecord);
                }
                return;
            }

            //查出邀请中最近的一个，添加到头部。
            int gpinvite = 0;
            sjtu.opennet.textilepb.View.InviteView lastInvite = null;
            try {
                if (Textile.instance().invites != null) {
                    List<View.InviteView> invites = Textile.instance().invites.list().getItemsList();
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
                TDialog noti=new TDialog("",lastInvite.getInviter().getName()+" 邀请你",
                        lastInvite.getDate().getSeconds(),false,"tongzhi",true,true);
                EventBus.getDefault().post(noti);
            }
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
            synchronized (LOCK){
                if(lastBlock.equals(feedItemData.block)){
                    return;
                }else{
                    lastBlock=feedItemData.block;
                }
            }
            Model.Thread thread=null;
            try {
                thread=Textile.instance().threads.get(threadId);
                if ((thread != null) && thread.getSharing().equals(Model.Thread.Sharing.NOT_SHARED)){
                    return ;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            Message msg=threadUpdateHandler.obtainMessage();
            msg.obj=new ThreadUpdateWork(thread,feedItemData);
            threadUpdateHandler.sendMessage(msg);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void shutDown(Integer stop){
        if(stop==943){
            Log.d(TAG, "shutDown: 服务stop");
            Textile.instance().destroy();
            serviceOn=false;
            stopForeground(true);
            stopSelf();
        }
    }
}
