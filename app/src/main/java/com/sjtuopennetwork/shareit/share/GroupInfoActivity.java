package com.sjtuopennetwork.shareit.share;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.library.flowlayout.FlowLayoutManager;
import com.sjtuopennetwork.shareit.LogUtils;
import com.sjtuopennetwork.shareit.R;
import com.sjtuopennetwork.shareit.share.util.GroupMemberAdapter;

import java.util.List;

import sjtu.opennet.hon.Textile;
import sjtu.opennet.textilepb.Model;

public class GroupInfoActivity extends AppCompatActivity {
    private static final String TAG = "========================================";

    //UI控件
    LinearLayout add_members;
    LinearLayout del_members;
    LinearLayout set_admin;
    LinearLayout group_qrcode;
//    TextView leave_group;
    RecyclerView group_members;
    TextView group_mem_num;


    //内存数据
    String threadid;
    Model.Thread group_thread;
    List<Model.Peer> allMembers;
    List<Model.Peer> nonAdmins;
    List<Model.Peer> admins;

    //持久化存储
    public SharedPreferences pref;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_info);

        initUI();

        initData();

    }

    @Override
    protected void onStart() {
        super.onStart();

        showMembers();
    }

    public void showMembers(){
        //显示成员列表
        try {
            group_thread = Textile.instance().threads.get(threadid);
            if(!Textile.instance().threads.isAdmin(threadid,Textile.instance().profile.get().getId())){ //如果不是管理员
                del_members.setVisibility(View.GONE);
                set_admin.setVisibility(View.GONE);
            }
            allMembers=Textile.instance().threads.peers(threadid).getItemsList();
            nonAdmins=Textile.instance().threads.nonAdmins(threadid).getItemsList();
            admins=Textile.instance().threads.admins(threadid).getItemsList();
            LogUtils.d(TAG, "showMembers: "+allMembers.size());
        } catch (Exception e) {
            e.printStackTrace();
        }

        group_mem_num.setText("群成员 （"+allMembers.size()+" 人）");

        //显示成员列表
        GroupMemberAdapter adapter=new GroupMemberAdapter(this,allMembers);
        GridLayoutManager gridLayoutManager=new GridLayoutManager(this,5);
        group_members.setLayoutManager(gridLayoutManager);
        group_members.setAdapter(adapter);
    }

    private int dp2px(float value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }

    private void initUI() {
        add_members=findViewById(R.id.group_add_members);
        del_members=findViewById(R.id.group_del_members);
        set_admin=findViewById(R.id.group_set_admin);
//        leave_group=findViewById(R.id.leave_group);
        group_members=findViewById(R.id.group_members);
        group_qrcode=findViewById(R.id.group_qrcode);
        group_mem_num=findViewById(R.id.group_mem_num);
    }
    private void initData() {
        pref=getSharedPreferences("txtl", Context.MODE_PRIVATE);


        //获得管理员、非管理员，管理员才显示设置管理员。
        threadid=getIntent().getStringExtra("threadid");

        showMembers();

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
//        leave_group.setOnClickListener(v -> {
//            //还是先弹出对话框吧，防止误触
//            AlertDialog.Builder leave=new AlertDialog.Builder(GroupInfoActivity.this);
//            leave.setTitle("退出群组");
//            leave.setMessage("确定退出群组吗？");
//            leave.setPositiveButton("确定", (dialog, which) -> {
//                //自己退出群组,removeThread，自己removeThread
//                try {
//                    Textile.instance().threads.remove(threadid);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//                DBoperator.deleteDialogByThreadID(appdb,threadid);
//
//                //广播ChatActivity清除
//                Intent intent=new Intent(ChatActivity.REMOVE_DIALOG);
//                sendBroadcast(intent);
//
//                finish();
//            });
//            leave.setNegativeButton("取消", (dialog, which) -> Toast.makeText(GroupInfoActivity.this,"已取消",Toast.LENGTH_SHORT).show());
//            leave.show();
//        });

        group_qrcode.setOnClickListener(v -> {
            Intent it=new Intent(GroupInfoActivity.this,GroupCodeActivity.class);
            it.putExtra("threadid",threadid);
            startActivity(it);
        });
    }
}
