package com.sjtuopennetwork.shareit.share;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.LinearLayout;

import com.sjtuopennetwork.shareit.R;

import java.util.List;

import sjtu.opennet.hon.Textile;
import sjtu.opennet.textilepb.Model;

public class GroupInfoActivity extends AppCompatActivity {
    LinearLayout add_members;
    LinearLayout del_members;
    LinearLayout set_admin;
    LinearLayout leave_group;


    //内存数据
    String threadid;
    Model.Thread group_thread;
    List<Model.Peer> allMembers;
    List<Model.Peer> nonAdmins;
    List<Model.Peer> admins;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_info);

        initUI();

        initData();

    }
    private void initUI() {
        add_members=findViewById(R.id.group_add_members);
        del_members=findViewById(R.id.group_del_members);
        set_admin=findViewById(R.id.group_set_admin);
        leave_group=findViewById(R.id.group_leave);

    }
    private void initData() {
        //获得管理员、非管理员，管理员才显示设置管理员。
        threadid=getIntent().getStringExtra("threadid");
        try {
            group_thread = Textile.instance().threads.get(threadid);
            allMembers=Textile.instance().threads.peers(threadid).getItemsList();
            nonAdmins=Textile.instance().threads.nonAdmins(threadid).getItemsList();
            admins=Textile.instance().threads.admins(threadid).getItemsList();
        } catch (Exception e) {
            e.printStackTrace();
        }



        add_members.setOnClickListener(v -> {
            Intent it=new Intent(GroupInfoActivity.this,GroupAddMemberActivity.class);
            it.putExtra("threadid",threadid);
            startActivity(it);
        });
        del_members.setOnClickListener(v -> {
            Intent it=new Intent(GroupInfoActivity.this,GroupDelMemberActivity.class);
            it.putExtra("threadid",threadid);
            startActivity(it);
        });
        set_admin.setOnClickListener(v -> {
            Intent it=new Intent(GroupInfoActivity.this,GroupSetAdminActivity.class);
            it.putExtra("threadid",threadid);
            startActivity(it);
        });
    }


}
