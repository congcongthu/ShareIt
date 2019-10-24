package com.sjtuopennetwork.shareit.contact;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;

import com.sjtuopennetwork.shareit.R;

import java.util.LinkedList;
import java.util.List;

import io.textile.pb.Model;
import io.textile.pb.View;
import io.textile.textile.Textile;


public class NewFriendActivity extends AppCompatActivity {


    //内存数据
    List<View.InviteView> invites;
    List<View.InviteView> friendApplications;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_friend);

        friendApplications=new LinkedList<>();
        try {
            invites = Textile.instance().invites.list().getItemsList();
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
                    friendApplications.add(inviteView);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }


        //将通知显示到ListView中

    }
}
