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

public class GroupDelMemberActivity extends AppCompatActivity {

    //UI控件
    Button delete_members;
    ContactView contactView;

    //内存数据
    String threadid;
    List<ContactBean> contactBeans;
    List<Model.Peer> allMembers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_del_member);

        initUI();

        initData();

    }

    private void initUI() {
        delete_members=findViewById(R.id.delete_members);
        contactView=findViewById(R.id.group_delete_members);
    }

    private void initData() {
        contactBeans=new LinkedList<>();
        allMembers=new LinkedList<>();
        try {
            threadid=getIntent().getStringExtra("threadid");
            allMembers= Textile.instance().threads.peers(threadid).getItemsList();
        } catch (Exception e) {
            e.printStackTrace();
        }

        for(Model.Peer p:allMembers){
            ContactBean contactBean=new ContactBean();
            contactBean.setId(p.getId());
            contactBean.setName(p.getName());
            String avatarPath= FileUtil.getFilePath(p.getAvatar());
            contactBean.setAvatar(avatarPath);
            contactBeans.add(contactBean);
        }
        contactView.setData(contactBeans,true);
        contactView.setContactListener(new ContactListener<ContactBean>() {
            @Override
            public void onClick(ContactBean item) {
            }
            @Override
            public void onLongClick(ContactBean item) { }
            @Override
            public void loadAvatar(ImageView imageView, String avatar) { }
        });

        delete_members.setOnClickListener(v -> {
            List<ContactBean> selects=contactView.getChoostContacts();
            for(ContactBean c:selects){ //逐个删除
                try {
                    Textile.instance().threads.removePeer(threadid,c.getId());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            finish();
        });
    }
}
