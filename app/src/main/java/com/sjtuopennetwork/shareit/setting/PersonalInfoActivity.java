package com.sjtuopennetwork.shareit.setting;

import android.app.AlertDialog;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.entity.LocalMedia;
import com.luck.picture.lib.tools.PictureFileUtils;
import com.sjtuopennetwork.shareit.contact.util.ContactUtil;
import com.sjtuopennetwork.shareit.login.MainActivity;
import com.sjtuopennetwork.shareit.setting.util.NotificationGroup;
import com.sjtuopennetwork.shareit.setting.util.NotificationItem;
import com.sjtuopennetwork.shareit.setting.util.SwarmPeerListAdapter;
import com.sjtuopennetwork.shareit.util.CafeUtil;
import com.sjtuopennetwork.shareit.util.RoundImageView;
import com.sjtuopennetwork.shareit.util.ShareUtil;
import com.syd.oden.circleprogressdialog.core.CircleProgressDialog;
import com.sjtuopennetwork.shareit.R;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import sjtu.opennet.textilepb.Model;
import sjtu.opennet.hon.Handlers;
import sjtu.opennet.hon.Textile;

public class PersonalInfoActivity extends AppCompatActivity {

    private static final String TAG = "=========================";

    //UI控件
    private TextView info_name; //昵称
    private TextView info_addr; //公钥
    private TextView info_phrase; //助记词
    private TextView info_swarm_address;
    private TextView delay_time;
    private RoundImageView avatar_img;//头像
    private LinearLayout info_avatar_layout;   //头像板块
    private LinearLayout info_name_layout;  //昵称板块
    private LinearLayout info_addr_layout;  //公钥地址板块
    private LinearLayout info_phrase_layout;    //助记词板块
    private LinearLayout info_swarm_layout;   //swarm地址板块
    private LinearLayout info_swarm_peer_layout; //swarmPeer列表板块
    private LinearLayout info_cafe;
    private TextView cafeCaption;
    private TextView cafeUrl;
    CircleProgressDialog circleProgressDialog;

    private ExpandableListView swarm_peer_listView;
    //持久化
    private SharedPreferences pref;

    //内存
    private String username;
    private String useravatar;
    private String myaddr;
    private String phrase;
    private String swarm_address;
    private ArrayList<NotificationGroup> gData = null;
    private ArrayList<ArrayList<NotificationItem>> iData = null;
    private ArrayList<NotificationItem> swarm_peer_list = null;
    private Model.SwarmPeerList swarmPeerList;
    private List<Model.SwarmPeer> swarmPeers;
    private SwarmPeerListAdapter swarmPeerListAdapter=null;
    private Context mContext;
    private boolean connectCafe;
    boolean ok131;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_info);

        initUI();
        initData();
        drawUI();
    }
    private void drawUI() {
        info_name.setText(username);
        info_addr.setText(myaddr);
        info_phrase.setText(phrase);
        ShareUtil.setImageView(this,avatar_img,useravatar,0);
        info_swarm_address.setText(swarm_address);
    }

    Handler handler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            try{
                circleProgressDialog.dismiss();
            }catch (Exception e){
            }
            drawCafeInfo();
        }
    };

    public void drawCafeInfo(){
        Log.d(TAG, "drawCafeInfo: 打印cafe状态");
        ok131=pref.getBoolean("ok131",false);
        connectCafe=pref.getBoolean("connectCafe",false);
        if(ok131){
            cafeUrl.setVisibility(View.VISIBLE);
            cafeCaption.setText("cafe连接成功（点击停止）");
            cafeUrl.setText("http://202.120.38.131:40601");
        }else{
            cafeCaption.setText("未连接cafe（点击开始连接）");
            cafeUrl.setVisibility(View.GONE);
        }
    }

    private void initData() {
        pref=getSharedPreferences("txtl", Context.MODE_PRIVATE);

        drawCafeInfo();

        info_cafe.setOnClickListener(view -> {
            final android.app.AlertDialog.Builder dialog = new android.app.AlertDialog.Builder(PersonalInfoActivity.this);
            dialog.setTitle("Cafe连接");
            dialog.setPositiveButton("连接Cafe", (dialogInterface, i) -> {
                if(ok131==true){
                    Toast.makeText(PersonalInfoActivity.this, "已经连接了cafe", Toast.LENGTH_SHORT).show();
                }else{
                    circleProgressDialog=new CircleProgressDialog(PersonalInfoActivity.this);
                    circleProgressDialog.setText("连接cafe...");
                    circleProgressDialog.showDialog();

                    CafeUtil.connectCafe(new Handlers.ErrorHandler() {
                        @Override
                        public void onComplete() {
                            Log.d(TAG, "onComplete: cafe131成功");
                            connectCafe=true;
                            ok131=true;
                            SharedPreferences.Editor editor=pref.edit();
                            editor.putBoolean("131ok",ok131);
                            editor.putBoolean("connectCafe",connectCafe);
                            editor.commit();

                            Message msg=new Message(); msg.what=1; handler.sendMessage(msg);
                        }

                        @Override
                        public void onError(Exception e) {

                        }
                    });
                }
            });
            dialog.setNegativeButton("停止连接", (dialogInterface, i) -> {
                if(ok131==false){
                    Toast.makeText(PersonalInfoActivity.this, "已经停止", Toast.LENGTH_SHORT).show();
                }else{
                    try {
                        circleProgressDialog=new CircleProgressDialog(PersonalInfoActivity.this);
                        circleProgressDialog.setText("连接cafe...");
                        circleProgressDialog.showDialog();

                        CafeUtil.stopConnect(new Handlers.ErrorHandler() {
                            @Override
                            public void onComplete() {
                                Log.d(TAG, "onComplete: 停止cafe连接");
                                connectCafe=false;
                                ok131=false;
                                SharedPreferences.Editor editor=pref.edit();
                                editor.putBoolean("131ok",ok131);
                                editor.putBoolean("connectCafe",connectCafe);
                                editor.commit();

                                Message msg=new Message(); msg.what=1; handler.sendMessage(msg);
                            }

                            @Override
                            public void onError(Exception e) {

                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            AlertDialog d = dialog.create();
            d.show();
        });

        username= ShareUtil.getMyName();
        useravatar=ShareUtil.getMyAvatar();
        Log.d(TAG, "initData: name avatar: "+username+" "+useravatar);

        phrase=pref.getString("phrase","");
        try {
            myaddr=Textile.instance().profile.get().getAddress();
        } catch (Exception e) {
            e.printStackTrace();
        }
        String peerId;
        try {//获取自己的swarm地址
            peerId = Textile.instance().profile.get().getId();
            swarm_address=Textile.instance().ipfs.getSwarmAddress(peerId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        initSwarmPeerList();
    }

    //获取swarm列表
    private void initSwarmPeerList() {
        swarm_peer_list=new ArrayList<>();
        gData=new ArrayList<>();
        iData=new ArrayList<>();
        List<Model.Peer> friendList;
        friendList= ContactUtil.getFriendList();
        try {
            swarmPeerList=Textile.instance().ipfs.connectedAddresses();
            swarmPeers=swarmPeerList.getItemsList();
            gData.add(new NotificationGroup("swarm地址列表"+"  total: "+swarmPeers.size()));
            for(Model.SwarmPeer peer:swarmPeers){
                String avatar="";
                String name="";
                for(Model.Peer p:friendList){//如果是好友则显示头像和姓名
                    if(p.getId().equals(peer.getId())){
                        avatar=p.getAvatar();
                        name=p.getName();
                    }
                }
                swarm_peer_list.add(new NotificationItem(peer.getId(),peer.getLatency(),avatar,name,peer.getAddr(),peer.getDirection()));
            }
            iData.add(swarm_peer_list);
        } catch (Exception e) {
            e.printStackTrace();
        }
        mContext=PersonalInfoActivity.this;
        swarmPeerListAdapter = new SwarmPeerListAdapter(mContext,gData,iData);
        swarm_peer_listView.setAdapter(swarmPeerListAdapter);

        swarm_peer_listView.collapseGroup(0);
        swarm_peer_listView.setOnChildClickListener((expandableListView, view, i, i1, l) -> {
            String swarm_url=iData.get(0).get(i1).swarmAddress+"/ipfs/"+iData.get(0).get(i1).peerId;
            Log.d(TAG, "onChildClick: swarmAddress："+swarm_url);
            Toast.makeText(PersonalInfoActivity.this,"已将swarm地址复制到剪贴板："+swarm_url,Toast.LENGTH_LONG).show();
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            // 将文本内容放到系统剪贴板里。
            cm.setText(swarm_url);
            return false;
        });
    }

    private void initUI() {
        avatar_img = findViewById((R.id.setting_avatar_niceview));
        info_name = findViewById(R.id.info_name);
        info_addr = findViewById(R.id.info_addr);
        info_avatar_layout = findViewById(R.id.setting_personal_info_avatar);
        info_name_layout = findViewById(R.id.setting_personal_info_name);
        info_addr_layout = findViewById(R.id.setting_personal_info_address);
        info_phrase_layout=findViewById(R.id.setting_personal_info_phrase);
        info_swarm_layout=findViewById(R.id.setting_personal_info_swarm_address);
        info_phrase=findViewById(R.id.info_phrase);
        info_swarm_address=findViewById(R.id.info_swarm_address);
        swarm_peer_listView=findViewById(R.id.swarm_peer_list);
        info_swarm_peer_layout=findViewById(R.id.setting_personal_info_swarm_peer_list);
        delay_time=findViewById(R.id.noti_time);

        cafeCaption=findViewById(R.id.cafe_caption);
        cafeUrl=findViewById(R.id.personal_cafe_url);
        info_cafe=findViewById(R.id.personal_info_cafe);


        info_avatar_layout.setOnClickListener(v -> {
            com.luck.picture.lib.PictureSelector.create(PersonalInfoActivity.this)
                .openGallery(PictureMimeType.ofImage())
                .maxSelectNum(1)
                .compress(true)
                .enableCrop(true).withAspectRatio(1,1)
                .forResult(PictureConfig.TYPE_IMAGE);
        });

        info_name_layout.setOnClickListener(view -> {
            Intent it=new Intent(PersonalInfoActivity.this,InfoNameActivity.class);
            startActivity(it);
        });
        info_addr_layout.setOnClickListener(view -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            // 将文本内容放到系统剪贴板里。
            cm.setText(info_addr.getText());
            Toast.makeText(this,"已将地址复制到剪贴板："+info_addr.getText(),Toast.LENGTH_LONG).show();
        });
        info_phrase_layout.setOnClickListener(view -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            // 将文本内容放到系统剪贴板里。
            cm.setText(info_phrase.getText());
            Toast.makeText(this,"已将助记词复制到剪贴板："+info_phrase.getText(),Toast.LENGTH_LONG).show();
        });
        info_swarm_layout.setOnClickListener(view -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            // 将文本内容放到系统剪贴板里。
            cm.setText(info_swarm_address.getText());
            Toast.makeText(this,"已将swarm地址复制到剪贴板："+info_swarm_address.getText(),Toast.LENGTH_LONG).show();
        });
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        initUI();
        initData();
        drawUI();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        /*结果回调*/
        if (requestCode == PictureConfig.TYPE_IMAGE) {
            if (data != null) {
                List<LocalMedia> choosePic= com.luck.picture.lib.PictureSelector.obtainMultipleResult(data);
                String picturePath=choosePic.get(0).getCompressPath();

                Textile.instance().profile.setAvatar(picturePath, new Handlers.BlockHandler() {
                    @Override
                    public void onComplete(Model.Block block) {
                        System.out.println("头像设置成功！");
                    }

                    @Override
                    public void onError(Exception e) {
                        System.out.println("头像设置失败！");
                    }
                });
                avatar_img.setImageBitmap(BitmapFactory.decodeFile(picturePath));
                avatar_img.setCornerRadius(5);
            }
        }
    }


    @Override
    protected void onStop() {
        super.onStop();
    }
}
