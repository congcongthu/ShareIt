package com.sjtuopennetwork.shareit.contact;

import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Pair;
import android.widget.TextView;

import com.shehuan.niv.NiceImageView;
import com.sjtuopennetwork.shareit.R;
import com.sjtuopennetwork.shareit.contact.util.ContactUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import sjtu.opennet.textilepb.Model;
import sjtu.opennet.textilepb.QueryOuterClass;
import sjtu.opennet.hon.Handlers;
import sjtu.opennet.hon.Textile;

public class ScanResultActivity extends AppCompatActivity {

    //UI控件
    TextView add_friend;
    NiceImageView res_avatar;
    TextView res_name;
    TextView res_address;

    //内存数据
    String name;
    String avatar;
    private String address;
    private Model.Contact resultContact;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent() != null){
            Bundle bundle = getIntent().getExtras();
            address = bundle.getString("result");
        }

        //注册监听器
        if(!EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().register(this);
        }

        setContentView(R.layout.activity_scan_result);
        initUI();

        sendQuery(); //有可能结果很快找到，UI还未初始化，所以要放在UI初始化后面
    }

    private void initUI() {
        add_friend=findViewById(R.id.scan_result_add);
        res_avatar=findViewById(R.id.scan_result_avatar);
        res_name=findViewById(R.id.scan_result_name);
        res_address=findViewById(R.id.scan_result_addr);
    }

    //发送查询请求
    private void sendQuery() {
        QueryOuterClass.QueryOptions options = QueryOuterClass.QueryOptions.newBuilder()
                .setWait(10)
                .setLimit(1)
                .build();
        QueryOuterClass.ContactQuery query = QueryOuterClass.ContactQuery.newBuilder()
                .setAddress(address)
//                .setName("规划局")
                .build();
        try {
            Textile.instance().contacts.search(query, options);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    //得到搜索结果
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void getResult(Model.Contact c){
        resultContact=c;
        //这里再设置
        drawUI();
    }

    private void drawUI() {
        name=resultContact.getName();
        avatar=resultContact.getAvatar();
        address=resultContact.getAddress();

        res_name.setText(name);
        res_address.setText(address);
        String getAvatar="/ipfs/" + avatar + "/0/small/content";
        Textile.instance().ipfs.dataAtPath(getAvatar, new Handlers.DataHandler() {
            @Override
            public void onComplete(byte[] data, String media) {
                res_avatar.setImageBitmap(BitmapFactory.decodeByteArray(data,0,data.length));
            }
            @Override
            public void onError(Exception e) {
//                        vh.avatar.setImageResource(R.drawable.ic_default_avatar);
            }
        });

        add_friend.setOnClickListener(v -> {
            //点击就申请添加好友，逻辑与搜索结果界面的添加是相同的
//            createTwoPersonThread(resultContact.getName());
            ContactUtil.createTwoPersonThread(address);
        });
    }

    //双人thread创建成功后就发送邀请，用户看起来就是好友申请
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void sendInvite(String threadId){
        try {
//            Model.Thread t=Textile.instance().threads.get(threadId);
            Textile.instance().contacts.add(resultContact);
            Textile.instance().invites.add(threadId,address); //key就是联系人的address
            finish();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        if(EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().unregister(this);
        }
    }
}
