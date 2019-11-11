package com.sjtuopennetwork.shareit.contact;

import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.sjtuopennetwork.shareit.R;
import com.sjtuopennetwork.shareit.contact.util.DiscoverAdapter;
import com.sjtuopennetwork.shareit.contact.util.ContactUtil;
import com.sjtuopennetwork.shareit.contact.util.ResultAdapter;
import com.sjtuopennetwork.shareit.contact.util.ResultContact;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import sjtu.opennet.hon.Textile;
import sjtu.opennet.textilepb.Model;
import sjtu.opennet.textilepb.QueryOuterClass;

public class ContactDiscoverActivity extends AppCompatActivity {

    //UI控件
    ListView discover_lv;
    ResultAdapter searchResultAdapter;  //搜索结果适配器
    DiscoverAdapter discoverAdapter;
    Button createNewGroup;


    //内存数据
    List<Model.Peer> myFriends;
    List<Model.Contact> newContacts;  //搜索到的结果
    List<ResultContact> resultContacts;  //存放自定义的搜索结果item对象
    String targetAddress;
    List<Integer> listItemID;

    DiscoverAdapter.MyClickListener myClickListener=new DiscoverAdapter.MyClickListener() {
        @Override
        public void myOnClick(int position, View v) {
            Model.Contact wantToAdd=newContacts.get(position);
            AlertDialog.Builder addContact=new AlertDialog.Builder(ContactDiscoverActivity.this);
            addContact.setTitle("添加联系人");
            addContact.setMessage("确定添加 "+wantToAdd.getName()+" 吗？");
            addContact.setPositiveButton("添加", (dialog, which) -> {
                try {
                    Textile.instance().contacts.add(wantToAdd);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                targetAddress=wantToAdd.getAddress();
                ContactUtil.createTwoPersonThread(targetAddress); //创建双人thread,key就是那个人的地址
            });
            addContact.setNegativeButton("取消", (dialog, which) -> Toast.makeText(ContactDiscoverActivity.this,"已取消",Toast.LENGTH_SHORT).show());
            addContact.show();
        }
    };

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

        createNewGroup=findViewById(R.id.bt_create_new_group);

        discover_lv=findViewById(R.id.contact_discover_result_lv);
        myFriends= ContactUtil.getFriendList();

        resultContacts= Collections.synchronizedList(new LinkedList<>());
        newContacts=Collections.synchronizedList(new LinkedList<>());
        discoverAdapter=new DiscoverAdapter(this,resultContacts,myClickListener);
        discoverAdapter.notifyDataSetChanged();
        discover_lv.setAdapter(discoverAdapter);

        createNewGroup.setOnClickListener(view -> {
            //获得所有的选中的contact
            listItemID=new ArrayList<>();
            for(int i=0;i<discoverAdapter.mChecked.size();i++){
                if(discoverAdapter.mChecked.get(i)){
                    listItemID.add(i);
                }
            }

            //对话框得到群组名
            //先弹出对话框，输入thread名称之后获取到名称，然后调佣addNewThread方法
            final EditText newThreadEdit=new EditText(this);
            AlertDialog.Builder addThread=new AlertDialog.Builder(this);
            addThread.setTitle("新建群组");
            addThread.setView(newThreadEdit);
            addThread.setPositiveButton("创建", (dialogInterface, i) -> {
                String threadname=newThreadEdit.getText().toString();
                //创建多人thread
                ContactUtil.createMultiPersonThread(threadname);
            });
            addThread.setNegativeButton("取消", (dialog, which) -> Toast.makeText(this,"已取消",Toast.LENGTH_SHORT).show());
            addThread.show();
        });
    }


    //一次得到一个搜索结果
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void getAnResult(Model.Contact c){
        System.out.println("===========本地发现得到用户："+c.getName());

        //看是不是已经添加过了，如果是则过滤掉。
        for(Model.Contact contact:newContacts){
            if(contact.getAddress().equals(c.getAddress())){
                return;
            }
        }

        //看是不是自己的好友
        boolean isMyFriend=false;
        for(Model.Peer p:myFriends){
            if(p.getAddress().equals(c.getAddress())){
                isMyFriend=true; //如果已经添加过这个联系人直接返回
            }
        }

        try {
            Textile.instance().contacts.add(c); //添加进来，为了后面能够直接创建群组
        } catch (Exception e) {
            e.printStackTrace();
        }
        newContacts.add(c); //添加
        resultContacts.add(new ResultContact(c.getAddress(),c.getName(),c.getAvatar(),null,isMyFriend));
        discoverAdapter.mChecked.add(false);
        discover_lv.setSelection(0);
    }

    //双人thread创建成功后就发送邀请，用户看起来就是好友申请
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void sendInvite(String threadId){
        Model.Thread t=null;
        try {
            t=Textile.instance().threads.get(threadId);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(t.getWhitelistCount()==2){ //如果是添加好友的threadAdd
            try{
                Textile.instance().invites.add(threadId,targetAddress); //key就是联系人的address
                System.out.println("===============发送了邀请");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }else{ //如果是创建群组的threadAdd
            for (Integer integer:listItemID){
                System.out.println("================发现选中了："+integer);
                //逐个发邀请
                try {
                    Textile.instance().invites.add(threadId,resultContacts.get(integer).address);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                finish();
            }
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
