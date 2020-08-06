package com.sjtuopennetwork.shareit.share.multichat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.leon.lfilepickerlibrary.LFilePicker;
import com.luck.picture.lib.PictureSelector;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.entity.LocalMedia;
import com.sjtuopennetwork.shareit.R;
import com.sjtuopennetwork.shareit.share.util.TMsg;
import com.sjtuopennetwork.shareit.share.util.TMsgAdapter;
import com.sjtuopennetwork.shareit.util.DBHelper;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.List;

import sjtu.opennet.hon.MulticastFile;
import sjtu.opennet.hon.Textile;
import sjtu.opennet.multicast.MulticastHelper;
import sjtu.opennet.stream.util.FileUtil;

public class ChatMultiActivity extends AppCompatActivity {
    private static final String TAG = "============ChatMultiActivity";

    LinearLayout adding_multi_file;
    ImageView bt_add_file;
    Button chat_multi_send_text;
    TextView bt_multi_send_img;
    TextView bt_multi_send__file;
    ListView chat_multi_lv;
    EditText chat_multi_text_edt;

    public SharedPreferences pref;

    boolean addingFile = false;
    static final int MULTI_FILE=4785;
    static final int MULTI_PIC=4378;
    static final String MULTI_THREADID="20200729multicast";
    String loginAccount;
    String myName;
    List<TMsg> msgList;
    TMsgAdapter msgAdapter;
    int xiansuTime;
    List<String> chooseFilePath;
    List<LocalMedia> choosePic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_multi);

        initMulti();
    }

    @Override
    protected void onStart() {
        super.onStart();

        msgList= DBHelper.getInstance(getApplicationContext(),loginAccount).list3000Msg("20200729multicast");
        msgAdapter=new TMsgAdapter(this,msgList,"20200729multicast");
        chat_multi_lv.setAdapter(msgAdapter);
        chat_multi_lv.setSelection(msgList.size());

        if(!EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().register(this);
        }
    }

    private void initMulti() {
        pref=getSharedPreferences("txtl",MODE_PRIVATE);
        loginAccount=pref.getString("loginAccount",""); //当前登录的account，就是address

        try {
            myName= Textile.instance().profile.name();
        } catch (Exception e) {
            e.printStackTrace();
        }

        adding_multi_file=findViewById(R.id.file_multi_layout);
        bt_add_file=findViewById(R.id.bt_multi_add_file);
        chat_multi_send_text=findViewById(R.id.chat_multi_send_text);
        bt_multi_send_img=findViewById(R.id.bt_multi_send_img);
        bt_multi_send__file=findViewById(R.id.bt_multi_send__file);
        chat_multi_lv=findViewById(R.id.chat_multi_lv);
        chat_multi_text_edt=findViewById(R.id.chat_multi_text_edt);

        adding_multi_file.setVisibility(View.GONE);
        bt_add_file.setOnClickListener(view -> {
            if(addingFile){
                addingFile=false;
                adding_multi_file.setVisibility(View.GONE);
            }else{
                addingFile=true;
                adding_multi_file.setVisibility(View.VISIBLE);
            }
        });

        chat_multi_send_text.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String msgTxt=chat_multi_text_edt.getText().toString();
                if(msgTxt.equals("")){
                    Toast.makeText(ChatMultiActivity.this, "消息不能为空", Toast.LENGTH_SHORT).show();
                    return;
                }
                chat_multi_text_edt.setText("");
                TMsg tMsg= null;
                long nowTime=System.currentTimeMillis()/1000;
                try {
                    tMsg= DBHelper.getInstance(getApplicationContext(),loginAccount).insertMsg(
                            MULTI_THREADID,10,String.valueOf(nowTime),myName,msgTxt,nowTime,1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                msgList.add(tMsg);
                msgAdapter.notifyDataSetChanged();
                chat_multi_lv.setSelection(msgList.size());

                MulticastFile multicastFile=new MulticastFile(MULTI_THREADID,"",loginAccount,"",msgTxt,nowTime,0);
                xiansuTime=pref.getInt("xiansu",100);
                MulticastHelper.sendMulticastFile(xiansuTime,multicastFile);
            }
        });

        bt_multi_send_img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PictureSelector.create(ChatMultiActivity.this)
                        .openGallery(PictureMimeType.ofImage())
                        .maxSelectNum(1)
                        .compress(false)
                        .forResult(MULTI_PIC);
            }
        });

        bt_multi_send__file.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PopupMenu file_select_menu = new PopupMenu(ChatMultiActivity.this, view);
                file_select_menu.getMenuInflater().inflate(R.menu.file_select, file_select_menu.getMenu());
                file_select_menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        switch(menuItem.getItemId()){
                            case R.id.file_select_selector:
                                new LFilePicker()
                                        .withActivity(ChatMultiActivity.this)
                                        .withRequestCode(MULTI_FILE)
                                        .withMutilyMode(false)//false为单选
                                        .withTitle("文件选择")//标题
                                        .start();
                                break;
                            case R.id.file_select_generate:
                                EditText inputs = new EditText(ChatMultiActivity.this);
                                inputs.setInputType(InputType.TYPE_CLASS_NUMBER);
                                //inputs.setText("aa");
                                AlertDialog.Builder builder = new AlertDialog.Builder(ChatMultiActivity.this);
                                builder.setTitle("文件大小(KB)").setView(inputs)
                                        .setNegativeButton("取消", (dialogInterface, i) -> dialogInterface.dismiss());
                                builder.setPositiveButton("确定", (dialogInterface, i) -> {
                                    String tmp=inputs.getText().toString();
                                    if(tmp.equals("")){
                                        Toast.makeText(ChatMultiActivity.this, "不能为空", Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    int inputSize = Integer.parseInt(tmp);
                                    if (inputSize > 0) {
                                        String outDir = FileUtil.getAppExternalPath(ChatMultiActivity.this, "generatedFile");
                                        String testFilePath = FileUtil.generateTestFile(outDir, inputSize);
                                        Log.d(TAG, "onActivityResult: get file path: "+testFilePath);

                                        String fileName=new File(testFilePath).getName();
                                        TMsg tMsg= null;
                                        long nowTime=System.currentTimeMillis()/1000;
                                        try {
                                            tMsg= DBHelper.getInstance(getApplicationContext(),loginAccount).insertMsg(
                                                    MULTI_THREADID,9,String.valueOf(nowTime),myName,fileName,nowTime,1);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                        msgList.add(tMsg);
                                        msgAdapter.notifyDataSetChanged();
                                        chat_multi_lv.setSelection(msgList.size());

                                        MulticastFile multicastFile=new MulticastFile(MULTI_THREADID,"",loginAccount,fileName,testFilePath,nowTime,2);
                                        xiansuTime=pref.getInt("xiansu",100);
                                        MulticastHelper.sendMulticastFile(xiansuTime,multicastFile);
                                    }
                                });
                                builder.show();
                        }

                        return false;
                    }
                });

                file_select_menu.show();
            }
        });

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void updateChat(TMsg tMsg) {
        if (tMsg.threadid.equals(MULTI_THREADID)) {
            Log.d(TAG, "updateChat: " + tMsg.msgtype + " " + tMsg.body);

            msgList.add(tMsg);
            msgAdapter.notifyDataSetChanged();
            chat_multi_lv.setSelection(msgList.size());
        }
    }

            @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode == MULTI_FILE && resultCode==RESULT_OK){
            chooseFilePath = data.getStringArrayListExtra("paths");
            String filePath = chooseFilePath.get(0);
            Log.d(TAG, "onActivityResult: get multicast file: "+filePath);

            String fileName=new File(filePath).getName();
            TMsg tMsg= null;
            long nowTime=System.currentTimeMillis()/1000;
            try {
                tMsg=DBHelper.getInstance(getApplicationContext(),loginAccount).insertMsg(
                        MULTI_THREADID,9,String.valueOf(nowTime),myName,fileName,nowTime,1);
            } catch (Exception e) {
                e.printStackTrace();
            }
            msgList.add(tMsg);
            msgAdapter.notifyDataSetChanged();
            chat_multi_lv.setSelection(msgList.size());

            MulticastFile multicastFile=new MulticastFile(MULTI_THREADID,"",loginAccount,fileName,filePath,nowTime,2);
            xiansuTime=pref.getInt("xiansu",100);
            MulticastHelper.sendMulticastFile(xiansuTime,multicastFile);
        }else if(requestCode == MULTI_PIC && resultCode==RESULT_OK){
            choosePic=PictureSelector.obtainMultipleResult(data);
            String filePath=choosePic.get(0).getPath();

            String fileName=new File(filePath).getName();
            TMsg tMsg= null;
            long nowTime=System.currentTimeMillis()/1000;
            try {
                tMsg=DBHelper.getInstance(getApplicationContext(),loginAccount).insertMsg(
                        MULTI_THREADID,11,String.valueOf(nowTime),myName,filePath,nowTime,1);
            } catch (Exception e) {
                e.printStackTrace();
            }
            msgList.add(tMsg);
            msgAdapter.notifyDataSetChanged();
            chat_multi_lv.setSelection(msgList.size());

            MulticastFile multicastFile=new MulticastFile(MULTI_THREADID,"",loginAccount,fileName,filePath,nowTime,1);
            xiansuTime=pref.getInt("xiansu",100);
            MulticastHelper.sendMulticastFile(xiansuTime,multicastFile);
        }
    }
}
