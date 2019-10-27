package com.sjtuopennetwork.shareit.contact;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Pair;
import android.widget.TextView;

import com.sjtuopennetwork.shareit.R;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import io.textile.pb.Model;
import io.textile.pb.QueryOuterClass;
import io.textile.textile.Textile;

public class ScanResultActivity extends AppCompatActivity {

    //UI控件
    TextView add_friend;

    //内存数据
    private String address;
    private Model.Contact resultContact;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_result);

        if (getIntent() != null){
            Bundle bundle = getIntent().getExtras();
            address = bundle.getString("result");
        }

        if(!EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().register(this);
        }

        initUI();

        sendQuery();
    }

    private void initUI() {

    }

    //发送查询请求
    private void sendQuery() {
        QueryOuterClass.QueryOptions options = QueryOuterClass.QueryOptions.newBuilder()
                .setWait(10)
                .setLimit(1)
                .build();
        QueryOuterClass.ContactQuery query = QueryOuterClass.ContactQuery.newBuilder()
                .setAddress(address)
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

        add_friend.setOnClickListener(v -> {
            //点击就申请添加好友，逻辑与搜索结果界面的添加是相同的
            createTwoPersonThread(resultContact.getName(),resultContact.getAddress());
        });
    }


    //创建一个新的双人thread
    private void createTwoPersonThread(String threadName,String key){
        io.textile.pb.View.AddThreadConfig.Schema schema= io.textile.pb.View.AddThreadConfig.Schema.newBuilder()
                .setPreset(io.textile.pb.View.AddThreadConfig.Schema.Preset.MEDIA)
                .build();
        io.textile.pb.View.AddThreadConfig config=io.textile.pb.View.AddThreadConfig.newBuilder()
                .setSharing(Model.Thread.Sharing.SHARED)
                .setType(Model.Thread.Type.OPEN)
                .setKey(key).setName(threadName)
                .setSchema(schema)
                .build();
        try {
            Textile.instance().threads.add(config);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //双人thread创建成功后就发送邀请，用户看起来就是好友申请
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void sendInvite(Pair<Integer,String> addFriend){
        if(addFriend.first!=1){
            return;
        }
        try {
            Model.Thread t=Textile.instance().threads.get(addFriend.second);
            Textile.instance().invites.add(t.getId(),t.getKey()); //key就是联系人的address
            System.out.println("===============发送了邀请");
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
