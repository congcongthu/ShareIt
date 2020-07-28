package com.sjtuopennetwork.shareit.share;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.util.Pair;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.ByteString;
import com.leon.lfilepickerlibrary.LFilePicker;
import com.luck.picture.lib.PictureSelector;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.entity.LocalMedia;
import com.sjtuopennetwork.shareit.R;
import com.sjtuopennetwork.shareit.login.MainActivity;
import com.sjtuopennetwork.shareit.share.util.TMsg;
import com.sjtuopennetwork.shareit.share.util.TMsgAdapter;
import com.sjtuopennetwork.shareit.util.DBHelper;
import com.sjtuopennetwork.shareit.util.ShareUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.List;

import mobile.HlogHandler;
import sjtu.opennet.hon.MulticastFile;
import sjtu.opennet.multicast.FileMultiCaster;
import sjtu.opennet.stream.file.FilePusher;
import sjtu.opennet.stream.util.FileUtil;
import sjtu.opennet.stream.video.VideoPusher;
import sjtu.opennet.stream.video.ticketvideo.VideoSender_tkt;
import sjtu.opennet.textilepb.Model;
import sjtu.opennet.hon.Handlers;
import sjtu.opennet.hon.Textile;


public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "======================";

    //UI控件
    TextView chat_name_toolbar;
    ListView chat_lv;
    Button send_msg;
    ImageView bt_add_file;
    EditText chat_text_edt;
    ImageView group_menu;
    LinearLayout add_file_layout;
    TextView bt_send_img;
    LinearLayout chat_backgroud;
    TextView bt_send_video_stream;
    TextView bt_send_file;
    TextView bt_send_video_ticket;
    TextView bt_send_stream_pic;
    TextView bt_send_stream_file;
    TextView bt_send_multicast_file;

    //持久化数据
    public SharedPreferences pref;

    //内存数据
    String loginAccount; //当前登录的帐户
    boolean addingFile = false;
    String threadid;
    Model.Thread chat_thread;
    List<TMsg> msgList;
    TMsgAdapter msgAdapter;
    List<LocalMedia> choosePic;
    List<LocalMedia> chooseVideo;
    String myName;
    String myAvatar;
    List<String> chooseFilePath;
    long msgT1;
    int xiansuTime;

    //const
    int TICKET_VIDEO=1293;
    int SIMPLE_FILE=293;
    int STREAM_PIC=1871;
    int STREAM_FILE=756;
    int MULTI_FILE=7568;

    //退出群组相关
    public static final String REMOVE_DIALOG="you get out";
    FinishActivityRecevier finishActivityRecevier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        initUI();

        initData();

        //注册广播
        finishActivityRecevier=new FinishActivityRecevier();
        IntentFilter intentFilter=new IntentFilter();
        intentFilter.addAction(REMOVE_DIALOG);
        registerReceiver(finishActivityRecevier,intentFilter);

    }
    @Override
    protected void onStart() {
        super.onStart();

        drawUI();

        if(!EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().register(this);
        }
    }

    private void drawUI() {
        msgList= DBHelper.getInstance(getApplicationContext(),loginAccount).list3000Msg(threadid);
        msgAdapter=new TMsgAdapter(this,msgList,threadid);
        chat_lv.setAdapter(msgAdapter);
        chat_lv.setSelection(msgList.size());
    }

    private void initUI() {
        chat_lv=findViewById(R.id.chat_lv);
        bt_send_img=findViewById(R.id.bt_send_img);
        bt_send_video_stream=findViewById(R.id.bt_send_video_stream);
        bt_send_file=findViewById(R.id.bt_send_file);
        chat_name_toolbar=findViewById(R.id.chat_name_toolbar);
        send_msg=findViewById(R.id.chat_send_text);
        bt_add_file=findViewById(R.id.bt_add_file);
        chat_text_edt=findViewById(R.id.chat_text_edt);
        group_menu=findViewById(R.id.group_menu);
        add_file_layout=findViewById(R.id.file_layout);
        add_file_layout.setVisibility(View.GONE);
        chat_backgroud=findViewById(R.id.chat_backgroud);
        bt_send_video_ticket=findViewById(R.id.bt_send_video_ticket);
        bt_send_stream_pic=findViewById(R.id.bt_send_stream_pic);
        bt_send_stream_file=findViewById(R.id.bt_send_stream_file);
        bt_send_multicast_file=findViewById(R.id.bt_send_multicast_file);

        chat_backgroud.setOnClickListener(view -> {
            if(addingFile){
                addingFile=false;
                add_file_layout.setVisibility(View.GONE);
            }
        });

        bt_add_file.setOnClickListener(view -> {
            if(addingFile){
                addingFile=false;
                add_file_layout.setVisibility(View.GONE);
            }else{
                addingFile=true;
                add_file_layout.setVisibility(View.VISIBLE);
            }
        });

//        Textile.instance().getLog(new HlogHandler() {
//            @Override
//            public void handleLog(String s) {
//                System.out.println("================get log: "+s);
//            }
//
//            @Override
//            public void logEnd() {
//
//            }
//        });
    }

    private void initData() {
        pref=getSharedPreferences("txtl",MODE_PRIVATE);
        loginAccount=pref.getString("loginAccount",""); //当前登录的account，就是address

        //初始化对话
        Intent it=getIntent();
        threadid=it.getStringExtra("threadid");
        try {
            myName=Textile.instance().profile.name();
            myAvatar=Textile.instance().profile.avatar();
            chat_thread = Textile.instance().threads.get(threadid);
            if(chat_thread.getWhitelistCount()==2){ //如果是双人thread
                String chatName=it.getStringExtra("singleName");
                group_menu.setVisibility(View.GONE);
                chat_name_toolbar.setText(chatName);
            }else{
                chat_name_toolbar.setText(chat_thread.getName());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        send_msg.setOnClickListener(view -> {
            final String msg=chat_text_edt.getText().toString();
            if(!msg.equals("")){
                chat_text_edt.setText("");
                try {
                    Log.d(TAG, "initData: get the msg: "+msg);
                    msgT1=System.currentTimeMillis();
                    Textile.instance().messages.add(threadid, msg); // set worker 30
                } catch (Exception e) {
                    e.printStackTrace();
                }
                String[] msgwords=msg.split(" ");
                if(msgwords[0].equals("set")){
                    if(msgwords[1].equals("worker")){
                        int deg=Integer.parseInt(msgwords[2]);
                        Textile.instance().streams.setDegree(deg);
                        Toast.makeText(this, "set worker "+deg+" successfully", Toast.LENGTH_SHORT).show();
                    }
                }
            }else{
                Toast.makeText(this,"消息不能为空", Toast.LENGTH_SHORT).show();
            }
        });

        bt_send_img.setOnClickListener(view -> {
            PictureSelector.create(ChatActivity.this)
                    .openGallery(PictureMimeType.ofImage())
                    .maxSelectNum(1)
                    .compress(false)
                    .forResult(PictureConfig.TYPE_IMAGE);
        });

        bt_send_video_stream.setOnClickListener(view -> {
            PictureSelector.create(ChatActivity.this)
                    .openGallery(PictureMimeType.ofVideo())
                    .maxSelectNum(1)
                    .compress(false)
                    .forResult(PictureConfig.TYPE_VIDEO);
        });
        bt_send_video_ticket.setOnClickListener(view -> {
            PictureSelector.create(ChatActivity.this)
                    .openGallery(PictureMimeType.ofVideo())
                    .maxSelectNum(1)
                    .compress(false)
                    .forResult(TICKET_VIDEO);
        });

        bt_send_file.setOnClickListener(v->{
            PopupMenu file_select_menu = new PopupMenu(ChatActivity.this, v);
            file_select_menu.getMenuInflater().inflate(R.menu.file_select, file_select_menu.getMenu());
            file_select_menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    switch(menuItem.getItemId()){
                        case R.id.file_select_selector:
                            new LFilePicker()
                                    .withActivity(ChatActivity.this)
                                    .withRequestCode(SIMPLE_FILE)
                                    .withMutilyMode(false)//false为单选
//                    .withFileFilter(new String[]{".txt",".png",".jpeg",".jpg" })//设置可选文件类型
                                    .withTitle("文件选择")//标题
                                    .start();
                            break;
                        case R.id.file_select_generate:
                            EditText inputs = new EditText(ChatActivity.this);
                            inputs.setInputType(InputType.TYPE_CLASS_NUMBER);
                            //inputs.setText("aa");
                            AlertDialog.Builder builder = new AlertDialog.Builder(ChatActivity.this);
                            builder.setTitle("文件大小(KB)").setView(inputs)
                                    .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            dialogInterface.dismiss();
                                        }
                                    });
                            builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    int inputSize = Integer.parseInt(inputs.getText().toString());
                                    if (inputSize > 0) {
                                        String outDir = FileUtil.getAppExternalPath(ChatActivity.this, "generatedFile");
                                        String testFilePath = FileUtil.generateTestFile(outDir, inputSize);
                                        Textile.instance().files.addSimpleFile(testFilePath, threadid, new Handlers.BlockHandler() {
                                            long addT1=System.currentTimeMillis();
                                            @Override
                                            public void onComplete(Model.Block block) {
                                                Log.d(TAG, "onComplete: 发送文件成功");
                                                long addT2=System.currentTimeMillis();
                                                try {
//                        String bbb=Textile.instance().files.list(threadid, "", 1).getItemsList().get(0).getBlock();
                                                    String bbb=block.getId();
                                                    Log.d(TAG, "onComplete: bbb: "+bbb);
                                                    DBHelper.getInstance(getApplicationContext(),loginAccount).recordLocalStartAdd(bbb,addT1,addT2);
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                }
                                            }

                                            @Override
                                            public void onError(Exception e) {

                                            }
                                        });
                                    }
                                }
                            });
                            builder.show();
                    }

                    return false;
                }
            });

            file_select_menu.show();
            /*
            new LFilePicker()
                    .withActivity(ChatActivity.this)
                    .withRequestCode(293)
                    .withMutilyMode(false)//false为单选
//                    .withFileFilter(new String[]{".txt",".png",".jpeg",".jpg" })//设置可选文件类型
                    .withTitle("文件选择")//标题
                    .start();

             */
        });

        bt_send_stream_pic.setOnClickListener(v->{
            PictureSelector.create(ChatActivity.this)
                    .openGallery(PictureMimeType.ofImage())
                    .maxSelectNum(1)
                    .compress(false)
                    .forResult(STREAM_PIC);
        });

        bt_send_stream_file.setOnClickListener(v->{
            PopupMenu file_select_menu = new PopupMenu(ChatActivity.this, v);
            file_select_menu.getMenuInflater().inflate(R.menu.file_select, file_select_menu.getMenu());
            file_select_menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    switch(menuItem.getItemId()){
                        case R.id.file_select_selector:
                            new LFilePicker()
                                    .withActivity(ChatActivity.this)
                                    .withRequestCode(STREAM_FILE)
                                    .withMutilyMode(false)//false为单选
//                    .withFileFilter(new String[]{".txt",".png",".jpeg",".jpg" })//设置可选文件类型
                                    .withTitle("文件选择")//标题
                                    .start();
                            break;
                        case R.id.file_select_generate:
                            EditText inputs = new EditText(ChatActivity.this);
                            inputs.setInputType(InputType.TYPE_CLASS_NUMBER);
                            //inputs.setText("aa");
                            AlertDialog.Builder builder = new AlertDialog.Builder(ChatActivity.this);
                            builder.setTitle("文件大小(KB)").setView(inputs)
                                    .setNegativeButton("取消", (dialogInterface, i) -> dialogInterface.dismiss());
                            builder.setPositiveButton("确定", (dialogInterface, i) -> {
                                String tmp=inputs.getText().toString();
                                if(tmp.equals("")){
                                    Toast.makeText(ChatActivity.this, "不能为空", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                int inputSize = Integer.parseInt(tmp);
                                if (inputSize > 0) {
                                    String outDir = FileUtil.getAppExternalPath(ChatActivity.this, "generatedFile");
                                    String testFilePath = FileUtil.generateTestFile(outDir, inputSize);
                                    Log.d(TAG, "onActivityResult: get stream file path: "+testFilePath);
                                    String fileHash=pushStreamFile(threadid, testFilePath,false);
                                    TMsg tMsg= null;
                                    try {
                                        long l=System.currentTimeMillis()/1000;
                                        tMsg=DBHelper.getInstance(getApplicationContext(),loginAccount).insertMsg(
                                                threadid,8,String.valueOf(l),myName,fileHash+"##"+testFilePath,l,1);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    msgList.add(tMsg);
                                    msgAdapter.notifyDataSetChanged();
                                    chat_lv.setSelection(msgList.size());

                                    Intent itToFileTrans=new Intent(ChatActivity.this, FileTransActivity.class);
                                    itToFileTrans.putExtra("fileCid",fileHash);
                                    itToFileTrans.putExtra("fileSizeCid",testFilePath);
                                    itToFileTrans.putExtra("isStream",true);
                                    startActivity(itToFileTrans);
                                }
                            });
                            builder.show();
                    }

                    return false;
                }
            });

            file_select_menu.show();

        });

        bt_send_multicast_file.setOnClickListener(v->{
            PopupMenu file_select_menu = new PopupMenu(ChatActivity.this, v);
            file_select_menu.getMenuInflater().inflate(R.menu.file_select, file_select_menu.getMenu());
            file_select_menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    switch(menuItem.getItemId()){
                        case R.id.file_select_selector:
                            new LFilePicker()
                                    .withActivity(ChatActivity.this)
                                    .withRequestCode(MULTI_FILE)
                                    .withMutilyMode(false)//false为单选
                                    .withTitle("文件选择")//标题
                                    .start();
                            break;
                        case R.id.file_select_generate:
                            EditText inputs = new EditText(ChatActivity.this);
                            inputs.setInputType(InputType.TYPE_CLASS_NUMBER);
                            //inputs.setText("aa");
                            AlertDialog.Builder builder = new AlertDialog.Builder(ChatActivity.this);
                            builder.setTitle("文件大小(KB)").setView(inputs)
                                    .setNegativeButton("取消", (dialogInterface, i) -> dialogInterface.dismiss());
                            builder.setPositiveButton("确定", (dialogInterface, i) -> {
                                String tmp=inputs.getText().toString();
                                if(tmp.equals("")){
                                    Toast.makeText(ChatActivity.this, "不能为空", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                int inputSize = Integer.parseInt(tmp);
                                if (inputSize > 0) {
                                    String outDir = FileUtil.getAppExternalPath(ChatActivity.this, "generatedFile");
                                    String testFilePath = FileUtil.generateTestFile(outDir, inputSize);
                                    Log.d(TAG, "onActivityResult: get file path: "+testFilePath);

                                    String fileName=new File(testFilePath).getName();
                                    TMsg tMsg= null;
                                    long nowTime=System.currentTimeMillis()/1000;
                                    try {
                                        tMsg=DBHelper.getInstance(getApplicationContext(),loginAccount).insertMsg(
                                                threadid,9,String.valueOf(nowTime),myName,fileName,nowTime,1);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    msgList.add(tMsg);
                                    msgAdapter.notifyDataSetChanged();
                                    chat_lv.setSelection(msgList.size());

                                    MulticastFile multicastFile=new MulticastFile(threadid,"",loginAccount,fileName,testFilePath,nowTime);
                                    xiansuTime=pref.getInt("xiansu",100);
                                    FileMultiCaster.sendMulticastFile(xiansuTime,multicastFile);
                                }
                            });
                            builder.show();
                    }

                    return false;
                }
            });

            file_select_menu.show();
        });

        group_menu.setOnClickListener(v -> {
            Intent toGroupInfo=new Intent(ChatActivity.this,GroupInfoActivity.class);
            toGroupInfo.putExtra("threadid",threadid);
            startActivity(toGroupInfo);
        });

        chat_lv.setOnItemLongClickListener((adapterView, view, position, l) -> {
            TMsg forward=msgList.get(position);
            Intent toForward =new Intent(ChatActivity.this,ForwardActivity.class);
            toForward.putExtra("msgType",forward.msgtype);
            String body="";
            switch (forward.msgtype){
                case 0: // 文本消息
                    toForward.putExtra("textBody",forward.body);
                    startActivity(toForward);
                    break;
                case 1: //图片
                    toForward.putExtra("picHashName",forward.body);
                    startActivity(toForward);
                    break;
                case 2: //stream视频
                    if(forward.ismine){ // msg内容是 posterPath filePath streamId
                        toForward.putExtra("streamIsMine",true);
                    }
                    else{ // msg内容是 posterId streamId
                    }
                    toForward.putExtra("streamBody",forward.body);
                    startActivity(toForward);
                    break;
                case 3: //file
//                    toForward.putExtra("fileHashName",forward.body);
//                    startActivity(toForward);
                    break;
                case 4: //ticket视频
                    Toast.makeText(this, "无法转发ticket视频", Toast.LENGTH_SHORT).show();
                    break;
            }
            return false;
        });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void updateChat(TMsg tMsg){
        if(tMsg.threadid.equals(threadid)){
            Log.d(TAG, "updateChat: "+tMsg.msgtype+" "+tMsg.body);

//            String blockId=tMsg.blockid;
//            int i=0;
//            for(;i<msgList.size();i++){
//                if(msgList.get(i).blockid.equals(blockId)){
//                    break;
//                }
//            }
//            if(i<msgList.size()){
//                msgList.remove(i);
//                msgList.add(i,tMsg);
//            }else{
//                msgList.add(tMsg);
//            }
            msgList.add(tMsg);

            msgAdapter.notifyDataSetChanged();
            chat_lv.setSelection(msgList.size());

            if((tMsg.msgtype==6 || tMsg.msgtype==5) && tMsg.ismine){
                String[] hashName=tMsg.body.split("##");
                Intent itToFileTrans=new Intent(this, FileTransActivity.class);
                itToFileTrans.putExtra("fileCid",hashName[2]);
                itToFileTrans.putExtra("fileSizeCid",hashName[0]);
                startActivity(itToFileTrans);
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void shadowDone(String s){
        if(s.equals("shadowDone")){
//            Toast.makeText(this, "备份完成", Toast.LENGTH_SHORT).show();
            AlertDialog.Builder upDone = new AlertDialog.Builder(this);
            upDone.setMessage("备份完成");
            upDone.setPositiveButton("确定",(dialog, which) -> {});
            upDone.show();
        }
    }

//    @Subscribe(threadMode = ThreadMode.MAIN)
//    public void testRTT(Long ack){
//        long rtt=ack-msgT1;
//        Log.d(TAG, "testRTT: rtt: "+rtt);
//    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==PictureConfig.TYPE_IMAGE && resultCode==RESULT_OK){
            choosePic=PictureSelector.obtainMultipleResult(data);
            String filePath=choosePic.get(0).getPath();
            String fileName=ShareUtil.getFileNameWithSuffix(filePath);
            Log.d(TAG, "onActivityResult: upload pic: "+fileName);

            Textile.instance().files.addSimplePicture(filePath, threadid, new Handlers.BlockHandler() {
                long addT1=System.currentTimeMillis();
                @Override
                public void onComplete(Model.Block block) {
                    Log.d(TAG, "onComplete: 发送图片成功");
                    long addT2=System.currentTimeMillis();
                    try {
                        String bbb=block.getId();
                        Log.d(TAG, "onComplete: bbb: "+bbb);
                        DBHelper.getInstance(getApplicationContext(),loginAccount).recordLocalStartAdd(bbb,addT1,addT2);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onError(Exception e) {

                }
            });

            //发送照片
//            Textile.instance().files.addPicture(filePath, threadid,fileName , new Handlers.BlockHandler() {
//                long addT1=System.currentTimeMillis();
//                @Override
//                public void onComplete(Model.Block block) {
//                    long addT2=System.currentTimeMillis();
//                    try {
//                        String bbb=Textile.instance().files.list(threadid, "", 1).getItemsList().get(0).getBlock();
//                        Log.d(TAG, "onComplete: bbb: "+bbb);
//                        DBHelper.getInstance(getApplicationContext(),loginAccount).recordLocalStartAdd(bbb,addT1,addT2);
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }
//                @Override
//                public void onError(Exception e) {
//                    e.printStackTrace();
//                }
//            });
        }else if(requestCode==PictureConfig.TYPE_VIDEO && resultCode==RESULT_OK){ //如果是选择了视频
            chooseVideo=PictureSelector.obtainMultipleResult(data);
            String filePath=chooseVideo.get(0).getPath();
            Log.d(TAG, "onActivityResult: 选择了视频："+filePath);

            VideoPusher videoPusher =new VideoPusher(this,threadid,filePath);
            videoPusher.startPush();

            Log.d(TAG, "onActivityResult: video added");
            //发送端立马显示发送视频
            String videoId= videoPusher.getVideoId();
            Bitmap tmpBmap = videoPusher.getPosterBitmap(); //拿到缩略图
            String tmpdir = ShareUtil.getAppExternalPath(this, "temp");
            String posterPath=tmpdir+"/"+videoId; //随机给一个名字
            ShareUtil.saveBitmap(posterPath,tmpBmap);
            String posterAndFile=posterPath+"##"+filePath+"##"+videoId;
            Log.d(TAG, "onActivityResult: stream video: "+posterAndFile);
            TMsg tMsg= null;
            try {
                long l=System.currentTimeMillis()/1000;
                tMsg=DBHelper.getInstance(getApplicationContext(),loginAccount).insertMsg(
                        threadid,2,String.valueOf(l),myName,posterAndFile,l,1);
            } catch (Exception e) {
                e.printStackTrace();
            }
            msgList.add(tMsg);
            msgAdapter.notifyDataSetChanged();
            chat_lv.setSelection(msgList.size());
        }else if(requestCode == TICKET_VIDEO &&resultCode == RESULT_OK){
            chooseVideo=PictureSelector.obtainMultipleResult(data);
            String filePath=chooseVideo.get(0).getPath();
            Log.d(TAG, "onActivityResult: 选择了视频："+filePath);

            VideoSender_tkt videoSenderTkt =new VideoSender_tkt(ChatActivity.this,threadid,filePath);
            videoSenderTkt.startSend();
//
            Log.d(TAG, "onActivityResult: video added");
            //发送端立马显示发送视频
            String videoId= videoSenderTkt.getVideoId();
            Bitmap tmpBmap = videoSenderTkt.getPosterBitmap(); //拿到缩略图
            String tmpdir = ShareUtil.getAppExternalPath(this, "temp");
            String posterPath=tmpdir+"/"+videoId; //随机给一个名字
            ShareUtil.saveBitmap(posterPath,tmpBmap);
            String posterAndFile=posterPath+"##"+filePath;
            Log.d(TAG, "onActivityResult: poster_file"+posterAndFile);
            TMsg tMsg= null;
            try {
                long l=System.currentTimeMillis()/1000;
                tMsg=DBHelper.getInstance(getApplicationContext(),loginAccount).insertMsg(
                        threadid,4,String.valueOf(l),myName,posterAndFile,l,1);
            } catch (Exception e) {
                e.printStackTrace();
            }
            msgList.add(tMsg);
            msgAdapter.notifyDataSetChanged();
            chat_lv.setSelection(msgList.size());
        }else if(requestCode == SIMPLE_FILE &&resultCode == RESULT_OK){
            chooseFilePath = data.getStringArrayListExtra("paths");
            String path = chooseFilePath.get(0);
            String chooseFileName=ShareUtil.getFileNameWithSuffix(path);

            Textile.instance().files.addSimpleFile(path, threadid, new Handlers.BlockHandler() {
                long addT1=System.currentTimeMillis();
                @Override
                public void onComplete(Model.Block block) {
                    Log.d(TAG, "onComplete: 发送文件成功");
                    long addT2=System.currentTimeMillis();
                    try {
//                        String bbb=Textile.instance().files.list(threadid, "", 1).getItemsList().get(0).getBlock();
                        String bbb=block.getId();
                        Log.d(TAG, "onComplete: bbb: "+bbb);
                        DBHelper.getInstance(getApplicationContext(),loginAccount).recordLocalStartAdd(bbb,addT1,addT2);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onError(Exception e) {

                }
            });

            //发送文件
//            Textile.instance().files.addFiles(chooseFilePath.get(0), threadid, chooseFileName, new Handlers.BlockHandler() {
//                long addT1=System.currentTimeMillis();
//                @Override
//                public void onComplete(Model.Block block) {
//                    long addT2=System.currentTimeMillis();
//                    try {
//                        String bbb=Textile.instance().files.list(threadid, "", 1).getItemsList().get(0).getBlock();
//                        Log.d(TAG, "onComplete: bbb: "+bbb);
//                        DBHelper.getInstance(getApplicationContext(),loginAccount).recordLocalStartAdd(bbb,addT1,addT2);
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }
//
//                @Override
//                public void onError(Exception e) {
//                    e.printStackTrace();
//                }
//            });
        }else if(requestCode == STREAM_PIC && resultCode==RESULT_OK){
            choosePic=PictureSelector.obtainMultipleResult(data);
            String filePath=choosePic.get(0).getPath();
            String picHash=pushStreamFile(threadid,filePath,true);

            TMsg tMsg= null;
            try {
                long l=System.currentTimeMillis()/1000;
                tMsg=DBHelper.getInstance(getApplicationContext(),loginAccount).insertMsg(
                        threadid,7,String.valueOf(l),myName,picHash+"##"+filePath,l,1);
            } catch (Exception e) {
                e.printStackTrace();
            }
            msgList.add(tMsg);
            msgAdapter.notifyDataSetChanged();
            chat_lv.setSelection(msgList.size());

            Intent itToFileTrans=new Intent(this, FileTransActivity.class);
            itToFileTrans.putExtra("fileCid",picHash);
            itToFileTrans.putExtra("fileSizeCid",filePath);
            itToFileTrans.putExtra("isStream",true);
            startActivity(itToFileTrans);
        }else if(requestCode == STREAM_FILE && resultCode==RESULT_OK){
            chooseFilePath = data.getStringArrayListExtra("paths");
            String filePath = chooseFilePath.get(0);

            Log.d(TAG, "onActivityResult: get stream file path: "+filePath);
            String fileHash=pushStreamFile(threadid,filePath,false);
            TMsg tMsg= null;
            try {
                long l=System.currentTimeMillis()/1000;
                tMsg=DBHelper.getInstance(getApplicationContext(),loginAccount).insertMsg(
                        threadid,8,String.valueOf(l),myName,fileHash+"##"+filePath,l,1);
            } catch (Exception e) {
                e.printStackTrace();
            }
            msgList.add(tMsg);
            msgAdapter.notifyDataSetChanged();
            chat_lv.setSelection(msgList.size());

            Intent itToFileTrans=new Intent(this, FileTransActivity.class);
            itToFileTrans.putExtra("fileCid",fileHash);
            itToFileTrans.putExtra("fileSizeCid",filePath);
            itToFileTrans.putExtra("isStream",true);
            startActivity(itToFileTrans);
        }else if(requestCode == MULTI_FILE && resultCode==RESULT_OK){
            chooseFilePath = data.getStringArrayListExtra("paths");
            String filePath = chooseFilePath.get(0);
            Log.d(TAG, "onActivityResult: get multicast file: "+filePath);

            String fileName=new File(filePath).getName();
            TMsg tMsg= null;
            long nowTime=System.currentTimeMillis()/1000;
            try {
                tMsg=DBHelper.getInstance(getApplicationContext(),loginAccount).insertMsg(
                        threadid,9,String.valueOf(nowTime),myName,fileName,nowTime,1);
            } catch (Exception e) {
                e.printStackTrace();
            }
            msgList.add(tMsg);
            msgAdapter.notifyDataSetChanged();
            chat_lv.setSelection(msgList.size());

            MulticastFile multicastFile=new MulticastFile(threadid,"",loginAccount,fileName,filePath,nowTime);
            xiansuTime=pref.getInt("xiansu",100);
            FileMultiCaster.sendMulticastFile(xiansuTime,multicastFile);
        }
    }

    @Override
    public void finish() {
        super.finish();

        //要把相应的Dialog表改为已读
        DBHelper.getInstance(getApplicationContext(),loginAccount).changeDialogRead(threadid,1);

    }

    @Override
    public void onStop() {

        super.onStop();

        if(EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().unregister(this);
        }
    }

    private class FinishActivityRecevier extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(REMOVE_DIALOG)){
                ChatActivity.this.finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(finishActivityRecevier);
    }

    public String pushStreamFile(String threadId, String path, boolean isPic) {
        File file=new File(path);
//        String streamId=String.valueOf(System.currentTimeMillis());
//        try {
//            streamId=ShareUtil.file2MD5(file);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        Log.d(TAG, "pushStreamFile: file md5: "+streamId);
        Model.StreamMeta.Type streamType;
        if(isPic){
            streamType=Model.StreamMeta.Type.PICTURE;
        }else{
            streamType=Model.StreamMeta.Type.FILE;
        }
        Log.d(TAG, "pushStreamFile: type: "+streamType);

//        Model.StreamMeta streamMeta = Model.StreamMeta.newBuilder()
//                .setId(streamId)
//                .setNsubstreams(1)
//                .setType(streamType)
//                .build();
//        try {
//            Textile.instance().streams.startStream(threadId, streamMeta);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        byte[] fileContent= FileUtil.readAllBytes(path);
        JSONObject object=new JSONObject();
        object.put("fileName",file.getName());
        String descStr= JSON.toJSONString(object);
        Model.StreamFile streamFile= Model.StreamFile.newBuilder()
                .setData(ByteString.copyFrom(fileContent))
                .setDescription(ByteString.copyFromUtf8(descStr))
                .build();

        String streamId="";
        try {
            Model.StreamMeta meta=Textile.instance().streams.fileAsStream(threadId,streamFile,streamType);
//            Textile.instance().streams.streamAddFile(streamId,streamFile.toByteArray());
//            Textile.instance().streams.closeStream(threadId,streamId);
            long addT1=System.currentTimeMillis();
            streamId= meta.getId();
            DBHelper.getInstance(getApplicationContext(),loginAccount).recordLocalStartAdd(streamId,addT1,0);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return streamId;
    }
}
