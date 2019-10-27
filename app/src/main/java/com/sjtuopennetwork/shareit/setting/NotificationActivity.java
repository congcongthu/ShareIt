package com.sjtuopennetwork.shareit.setting;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.sjtuopennetwork.shareit.R;

import java.util.List;

import io.textile.pb.Model;
import io.textile.pb.Model.Notification;
import io.textile.textile.Textile;

public class NotificationActivity extends AppCompatActivity {

    //UI控件
    private ListView listView;

    //内存
    private List<Notification> noti_list;
    private List<Notification> invite_noti_list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        initUI();
        initData();
        drawUI();
    }
    private void initUI() {
        listView=findViewById(R.id.notification_listView);
    }
    private void initData() {
        try {
            noti_list= Textile.instance().notifications.list("",10).getItemsList();
            for(Notification n:noti_list){
                n.getType().equals(Notification.Type.INVITE_RECEIVED);
                invite_noti_list.add(n);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void drawUI() {
        if (invite_noti_list != null) {
            ArrayAdapter<Notification> adapter = new ArrayAdapter<Notification>(
                    NotificationActivity.this, android.R.layout.simple_list_item_1, invite_noti_list);
            listView.setAdapter(adapter);
        }
    }
}
