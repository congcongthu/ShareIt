package com.sjtuopennetwork.shareit.share;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.leon.lfilepickerlibrary.LFilePicker;
import com.luck.picture.lib.PictureSelector;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.entity.LocalMedia;
import com.sjtuopennetwork.shareit.R;
import com.sjtuopennetwork.shareit.share.util.TMsg;
import com.sjtuopennetwork.shareit.share.util.TMsgAdapter;
import com.sjtuopennetwork.shareit.util.DBHelper;
import com.sjtuopennetwork.shareit.util.ShareUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

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

    //持久化数据
    public SharedPreferences pref;

    //内存数据
    String loginAccount; //当前登录的帐户
    boolean addingFile=false;
    String threadid;
    Model.Thread chat_thread;
    List<TMsg> msgList;
    TMsgAdapter msgAdapter;
    List<LocalMedia> choosePic;
    List<LocalMedia> chooseVideo;
    String myName;
    String myAvatar;
    List<String> chooseFilePath;


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
                    Textile.instance().messages.add(threadid, msg);
                } catch (Exception e) {
                    e.printStackTrace();
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
                    .forResult(1293);
        });

        bt_send_file.setOnClickListener(v->{
            new LFilePicker()
                    .withActivity(ChatActivity.this)
                    .withRequestCode(293)
                    .withMutilyMode(false)//false为单选
//                    .withFileFilter(new String[]{".txt",".png",".jpeg",".jpg" })//设置可选文件类型
                    .withTitle("文件选择")//标题
                    .start();
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
                    toForward.putExtra("fileHashName",forward.body);
                    startActivity(toForward);
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
            msgList.add(tMsg);
            msgAdapter.notifyDataSetChanged();
            chat_lv.setSelection(msgList.size());

            if(tMsg.msgtype==3 && tMsg.ismine){
                String[] hashName=tMsg.body.split("##");
                Intent itToFileTrans=new Intent(this, FileTransActivity.class);
                itToFileTrans.putExtra("fileCid",hashName[2]);
                startActivity(itToFileTrans);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==PictureConfig.TYPE_IMAGE && resultCode==RESULT_OK){
            choosePic=PictureSelector.obtainMultipleResult(data);
            String filePath=choosePic.get(0).getPath();
            String fileName=ShareUtil.getFileNameWithSuffix(filePath);
            Log.d(TAG, "onActivityResult: upload pic: "+fileName);
            //发送照片
            Textile.instance().files.addPicture(filePath, threadid,fileName , new Handlers.BlockHandler() {
                long addT1=System.currentTimeMillis();
                @Override
                public void onComplete(Model.Block block) {
                    long addT2=System.currentTimeMillis();
                    try {
                        String bbb=Textile.instance().files.list(threadid, "", 1).getItemsList().get(0).getBlock();
                        Log.d(TAG, "onComplete: bbb: "+bbb);
                        DBHelper.getInstance(getApplicationContext(),loginAccount).recordLocalStartAdd(bbb,addT1,addT2);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void onError(Exception e) {
                    e.printStackTrace();
                }
            });
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
        }else if(requestCode == 1293 &&resultCode == RESULT_OK){
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
        }else if(requestCode == 293 &&resultCode == RESULT_OK){
            chooseFilePath = data.getStringArrayListExtra("paths");
            String path = chooseFilePath.get(0);
            String chooseFileName=ShareUtil.getFileNameWithSuffix(path);

            //发送文件
            Textile.instance().files.addFiles(chooseFilePath.get(0), threadid, chooseFileName, new Handlers.BlockHandler() {
                long addT1=System.currentTimeMillis();
                @Override
                public void onComplete(Model.Block block) {
                    long addT2=System.currentTimeMillis();
                    try {
                        String bbb=Textile.instance().files.list(threadid, "", 1).getItemsList().get(0).getBlock();
                        Log.d(TAG, "onComplete: bbb: "+bbb);
                        DBHelper.getInstance(getApplicationContext(),loginAccount).recordLocalStartAdd(bbb,addT1,addT2);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onError(Exception e) {
                    e.printStackTrace();
                }
            });
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
}
