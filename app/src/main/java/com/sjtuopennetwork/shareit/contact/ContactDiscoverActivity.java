package com.sjtuopennetwork.shareit.contact;

import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.Toast;

import com.sjtuopennetwork.shareit.R;
import com.sjtuopennetwork.shareit.contact.util.GetFriendListOrApplication;
import com.sjtuopennetwork.shareit.contact.util.ResultAdapter;
import com.sjtuopennetwork.shareit.contact.util.ResultContact;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import sjtu.opennet.hon.Textile;
import sjtu.opennet.textilepb.Model;
import sjtu.opennet.textilepb.QueryOuterClass;

public class ContactDiscoverActivity extends AppCompatActivity {

    //UI控件
    ListView discover_lv;
    ResultAdapter searchResultAdapter;  //搜索结果适配器


    //内存数据
    List<Model.Peer> myFriends;
    List<Model.Contact> newContacts;  //搜索到的结果
    List<ResultContact> resultContacts;  //存放自定义的搜索结果item对象
    String targetAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_discover);

        initData();

        if(!EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().register(this);
        }

        //搜索
        QueryOuterClass.QueryOptions options = QueryOuterClass.QueryOptions.newBuilder()
                .setWait(10)
                .setLimit(100)
                .build();
        try {
            Textile.instance().contacts.discover(options);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    void initData(){

        discover_lv=findViewById(R.id.contact_discover_result_lv);
        myFriends= GetFriendListOrApplication.getFriendList();

        resultContacts= Collections.synchronizedList(new LinkedList<>());
        newContacts=Collections.synchronizedList(new LinkedList<>());
        searchResultAdapter=new ResultAdapter(this,R.layout.item_contact_search_result,resultContacts);
        searchResultAdapter.notifyDataSetChanged();
        discover_lv.setAdapter(searchResultAdapter);

        discover_lv.setOnItemClickListener((parent, view, position, id) -> {
            Model.Contact wantToAdd=newContacts.get(position);
            AlertDialog.Builder addContact=new AlertDialog.Builder(this);
            addContact.setTitle("添加联系人");
            addContact.setMessage("确定添加 "+wantToAdd.getName()+" 吗？");
            addContact.setPositiveButton("添加", (dialog, which) -> {
                try {
                    Textile.instance().contacts.add(wantToAdd);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                targetAddress=wantToAdd.getAddress();
                createTwoPersonThread(wantToAdd.getName()); //创建双人thread,key就是那个人的地址
            });
            addContact.setNegativeButton("取消", (dialog, which) -> Toast.makeText(this,"已取消",Toast.LENGTH_SHORT).show());
            addContact.show();
        });
    }

    //一次得到一个搜索结果
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void getAnResult(Model.Contact c){
        //添加到结果列表
        System.out.println("========本地发现得到搜索结果："+c.getName()+" "+c.getAddress());

        //要过滤一下，首先看是不是自己的好友，如果是就过滤掉。
        for(Model.Peer p:myFriends){
            if(p.getAddress().equals(c.getAddress())){
                return; //如果已经添加过这个联系人直接返回
            }
        }
        //再看是不是已经添加过了，如果已经有了也要过滤掉。
        for(Model.Contact contact:newContacts){
            if(contact.getAddress().equals(c.getAddress())){
                return;
            }
        }

        newContacts.add(c); //添加
        resultContacts.add(new ResultContact(c.getAddress(),c.getName(),c.getAvatar(),null));
        discover_lv.setSelection(0);
    }


    //创建一个新的双人thread
    private void createTwoPersonThread(String threadName){
        sjtu.opennet.textilepb.View.AddThreadConfig.Schema schema=
                sjtu.opennet.textilepb.View.AddThreadConfig.Schema.newBuilder()
                        .setPreset(sjtu.opennet.textilepb.View.AddThreadConfig.Schema.Preset.MEDIA)
                        .build();
        sjtu.opennet.textilepb.View.AddThreadConfig config=sjtu.opennet.textilepb.View.AddThreadConfig.newBuilder()
                .setSharing(Model.Thread.Sharing.SHARED)
                .setType(Model.Thread.Type.OPEN)
                .setKey(targetAddress).setName(threadName)
                .addWhitelist(targetAddress).addWhitelist(Textile.instance().account.address()) //两个人添加到白名单
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
    public void sendInvite(String threadId){
        try{
            Textile.instance().invites.add(threadId,targetAddress); //key就是联系人的address
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
