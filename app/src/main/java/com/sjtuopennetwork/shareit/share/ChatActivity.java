package com.sjtuopennetwork.shareit.share;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.luck.picture.lib.PictureSelector;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.entity.LocalMedia;
import com.shehuan.niv.NiceImageView;
import com.sjtuopennetwork.shareit.R;
import com.sjtuopennetwork.shareit.share.util.MsgAdapter;
import com.sjtuopennetwork.shareit.share.util.TMsg;
import com.sjtuopennetwork.shareit.util.AppdbHelper;
import com.sjtuopennetwork.shareit.util.DBoperator;
import com.sjtuopennetwork.shareit.util.FileUtil;
import com.syd.oden.circleprogressdialog.core.CircleProgressDialog;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

import sjtu.opennet.textilepb.Model;
import sjtu.opennet.hon.Handlers;
import sjtu.opennet.hon.Textile;

public class ChatActivity extends AppCompatActivity {
    //UI控件
    TextView chat_name_toolbar;
    ListView chat_lv;
    Button send_msg;
    NiceImageView bt_send_img;
    EditText chat_text_edt;
    ImageView group_menu;

    //持久化数据
    public SQLiteDatabase appdb;
    public SharedPreferences pref;

    //内存数据
    String threadid;
    Model.Thread chat_thread;
    List<TMsg> msgList;
//    MsgAdapter msgAdapter;
    List<LocalMedia> choosePic;
    String avatarpath;

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

    private void initUI() {
        chat_lv=findViewById(R.id.chat_lv);
        chat_name_toolbar=findViewById(R.id.chat_name_toolbar);
        send_msg=findViewById(R.id.chat_send_text);
        bt_send_img=findViewById(R.id.bt_send_img);
        chat_text_edt=findViewById(R.id.chat_text_edt);
        group_menu=findViewById(R.id.group_menu);
    }

    private void initData() {
        pref=getSharedPreferences("txtl",MODE_PRIVATE);
        appdb=AppdbHelper.getInstance(this,pref.getString("loginAccount","")).getWritableDatabase();
        avatarpath=pref.getString("avatarpath","null");

        //初始化对话
        Intent it=getIntent();
        threadid=it.getStringExtra("threadid");
        try {
            chat_thread = Textile.instance().threads.get(threadid);
            if(chat_thread.getWhitelistCount()==2){ //如果是双人thread
                group_menu.setVisibility(View.GONE);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        chat_name_toolbar.setText(chat_thread.getName());

        send_msg.setOnClickListener(view -> {
            final String msg=chat_text_edt.getText().toString();

            if(!msg.equals("")){
                chat_text_edt.setText("");
                try {
                    Textile.instance().messages.add(threadid,msg);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                TMsg tMsg= null;
                try {
                    tMsg = new TMsg(1,threadid,0,"",
                            Textile.instance().profile.name(),Textile.instance().profile.avatar(),msg,System.currentTimeMillis()/1000,true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                msgList.add(tMsg);
                chat_lv.setSelection(msgList.size());
            }else{
                Toast.makeText(this,"消息不能为空", Toast.LENGTH_SHORT).show();
            }
        });

        bt_send_img.setOnClickListener(view -> {
            PictureSelector.create(ChatActivity.this)
                    .openGallery(PictureMimeType.ofImage())
                    .maxSelectNum(1)
                    .compress(false)
                    .forResult(PictureConfig.CHOOSE_REQUEST);
        });

        group_menu.setOnClickListener(v -> {
            Intent toGroupInfo=new Intent(ChatActivity.this,GroupInfoActivity.class);
            toGroupInfo.putExtra("threadid",threadid);
            startActivity(toGroupInfo);
        });
    }

    public void drawUI(){
        msgList= DBoperator.queryMsg(appdb,threadid);
        System.out.println("=============消息数："+msgList.size());

//        chat_lv=findViewById(R.id.chat_lv);
        MsgAdapter msgAdapter=new MsgAdapter(this,msgList,avatarpath);
        chat_lv.setAdapter(msgAdapter);
        chat_lv.invalidateViews();
        chat_lv.setSelection(msgList.size());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void updateChat(TMsg tMsg){
        if(tMsg.threadid.equals(threadid)){
            msgList.add(tMsg);
            chat_lv.invalidateViews(); //强制刷新
            chat_lv.setSelection(msgList.size()); //图片有时候不立即显示，因为Item大小完全相同。
            System.out.println("==================收到了消息："+tMsg.body);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==PictureConfig.CHOOSE_REQUEST && resultCode==RESULT_OK){
            choosePic=PictureSelector.obtainMultipleResult(data);
            String filePath=choosePic.get(0).getPath();
            //发送照片
            Textile.instance().files.addFiles(filePath, threadid, "", new Handlers.BlockHandler() {
                @Override
                public void onComplete(Model.Block block) {

                }
                @Override
                public void onError(Exception e) {
                    e.printStackTrace();
                }
            });
            TMsg tMsg= null;
            try {
                tMsg = new TMsg(1,threadid,1,"",
                        Textile.instance().profile.name(),Textile.instance().profile.avatar(),filePath,System.currentTimeMillis()/1000,true);
            } catch (Exception e) {
                e.printStackTrace();
            }
            msgList.add(tMsg);
            chat_lv.setSelection(msgList.size());
        }
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
