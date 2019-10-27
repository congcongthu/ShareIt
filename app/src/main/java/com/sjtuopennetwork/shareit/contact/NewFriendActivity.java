package com.sjtuopennetwork.shareit.contact;

import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.sjtuopennetwork.shareit.R;
import com.sjtuopennetwork.shareit.contact.util.ResultAdapter;
import com.sjtuopennetwork.shareit.contact.util.ResultContact;

import java.util.LinkedList;
import java.util.List;

import io.textile.pb.Model;
import io.textile.pb.View;
import io.textile.textile.Textile;


public class NewFriendActivity extends AppCompatActivity {

    //UI控件
    ListView new_friend_lv;

    //内存数据
    List<View.InviteView> invites;
    List<View.InviteView> friendApplications;
    List<ResultContact> applications;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_friend);

        initUI();

        //将通知显示到ListView中
        showApplications();

    }

    private void showApplications() {
        friendApplications=new LinkedList<>();
        applications=new LinkedList<>();
        try {
            invites = Textile.instance().invites.list().getItemsList();
            System.out.println("=============invite数量："+invites.size());
            for (View.InviteView inviteView : invites) {
                String friendaddress = inviteView.getInviter().getAddress();
                boolean isApplication = true;
                for (Model.Contact c : Textile.instance().contacts.list().getItemsList()) {
                    if (c.getAddress().equals(friendaddress)) { //如果查到是好友发来的通知就不添加
                        isApplication = false;
                        break;
                    }
                }
                if (isApplication) {
                    ResultContact resultContact=new ResultContact(friendaddress,inviteView.getInviter().getName(),inviteView.getInviter().getAvatar(),null);
                    applications.add(resultContact);
                    friendApplications.add(inviteView);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        ResultAdapter resultAdapter=new ResultAdapter(NewFriendActivity.this,R.layout.item_contact_search_result,applications);
        new_friend_lv.setAdapter(resultAdapter);

        new_friend_lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, android.view.View view, int position, long id) {
                View.InviteView inviteView=friendApplications.get(position);
                AlertDialog.Builder accept=new AlertDialog.Builder(NewFriendActivity.this);
                accept.setTitle("同意好友申请");
                accept.setMessage("确定接收 "+applications.get(position).name+" 的好友申请吗？");
                accept.setPositiveButton("接受", (dialog, which) -> {
                    try {
                        Textile.instance().invites.accept(inviteView.getId());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                accept.setNegativeButton("取消", (dialog, which) -> Toast.makeText(NewFriendActivity.this,"已取消",Toast.LENGTH_SHORT).show());
                accept.show();
            }
        });
    }

    private void initUI() {
        new_friend_lv=findViewById(R.id.new_friend_lv);
    }


}
