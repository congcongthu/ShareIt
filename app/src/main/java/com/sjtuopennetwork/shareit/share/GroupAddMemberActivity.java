package com.sjtuopennetwork.shareit.share;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;

import com.chezi008.libcontacts.bean.ContactBean;
import com.chezi008.libcontacts.listener.ContactListener;
import com.chezi008.libcontacts.widget.ContactView;
import com.sjtuopennetwork.shareit.R;
import com.sjtuopennetwork.shareit.contact.util.GetFriendListOrApplication;
import com.sjtuopennetwork.shareit.util.FileUtil;

import java.util.LinkedList;
import java.util.List;

import sjtu.opennet.hon.Textile;
import sjtu.opennet.textilepb.Model;

public class GroupAddMemberActivity extends AppCompatActivity {

    //UI控件
    Button invite_new_members;
    ContactView contactView;

    //内存数据
    String threadid;
    List<ContactBean> contactBeans;
    List<Model.Peer> allMembers;
    List<Model.Peer> myFriends;
    List<Model.Peer> new_peers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_add_member);

        initUI();

        initData();

    }

    private void initUI() {
        invite_new_members=findViewById(R.id.invite_new_members);
        contactView=findViewById(R.id.group_invite_others);
    }

    private void initData() {
        new_peers=new LinkedList<>();
        contactBeans=new LinkedList<>();
        allMembers=new LinkedList<>();
        myFriends=new LinkedList<>();
        try {
            threadid=getIntent().getStringExtra("threadid");
            allMembers= Textile.instance().threads.peers(threadid).getItemsList();
            myFriends= GetFriendListOrApplication.getFriendList();
        } catch (Exception e) {
            e.printStackTrace();
        }

        //求好友与群成员的差集
        for(int i=0;i<myFriends.size();i++){
            if(!allMembers.contains(myFriends.get(i))){
                new_peers.add(myFriends.get(i));
            }
        }
        for(Model.Peer p:new_peers){
            ContactBean contactBean=new ContactBean();
            contactBean.setId(p.getAddress());
            contactBean.setName(p.getName());
            String avatarPath= FileUtil.getFilePath(p.getAvatar());
            contactBean.setAvatar(avatarPath);
            contactBeans.add(contactBean);
        }
        contactView.setData(contactBeans,true);
        contactView.setContactListener(new ContactListener<ContactBean>() {
            @Override
            public void onClick(ContactBean item) { }
            @Override
            public void onLongClick(ContactBean item) { }
            @Override
            public void loadAvatar(ImageView imageView, String avatar) { }
        });


        invite_new_members.setOnClickListener(v -> {
            List<ContactBean> selects=contactView.getChoostContacts();
            for(ContactBean c:selects){ //逐个发送邀请
                try {
                    Textile.instance().invites.add(threadid,c.getId());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            finish();
        });
    }

}
