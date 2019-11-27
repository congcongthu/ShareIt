package com.sjtuopennetwork.shareit.contact;

import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Pair;
import android.widget.ListView;
import android.widget.Toast;

import com.sjtuopennetwork.shareit.R;
import com.sjtuopennetwork.shareit.contact.util.ContactUtil;
import com.sjtuopennetwork.shareit.contact.util.ResultAdapter;
import com.sjtuopennetwork.shareit.contact.util.ResultContact;

import java.util.List;

import sjtu.opennet.textilepb.View;
import sjtu.opennet.hon.Textile;


public class NewFriendActivity extends AppCompatActivity {

    //UI控件
    ListView new_friend_lv;

    //内存数据
    List<View.InviteView> friendApplications;
    List<ResultContact> applications;
    Pair<List<View.InviteView>,List<ResultContact>> result;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_friend);
        initUI();

        //将通知显示到ListView中
        showApplications();

    }

    private void showApplications() {
        result= ContactUtil.getApplication();
        friendApplications=result.first;
        applications=result.second;

        ResultAdapter resultAdapter=new ResultAdapter(NewFriendActivity.this,R.layout.item_contact_search_result,applications);
        new_friend_lv.setAdapter(resultAdapter);
        new_friend_lv.setOnItemClickListener((parent, view, position, id) -> {
            View.InviteView inviteView=friendApplications.get(position);
            ResultContact resultContact=applications.get(position);
            AlertDialog.Builder accept=new AlertDialog.Builder(NewFriendActivity.this);
            accept.setTitle("同意好友申请");
            accept.setMessage("确定接收 "+resultContact.name+" 的好友申请吗？");
            accept.setPositiveButton("接受", (dialog, which) -> {
                try {
                    Textile.instance().invites.accept(inviteView.getId());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            accept.setNegativeButton("取消", (dialog, which) -> Toast.makeText(NewFriendActivity.this,"已取消",Toast.LENGTH_SHORT).show());
            accept.show();
        });
    }

    private void initUI() {
        new_friend_lv=findViewById(R.id.new_friend_lv);
    }


}
