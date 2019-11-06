package com.sjtuopennetwork.shareit.setting;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.Toast;

import com.sjtuopennetwork.shareit.R;
import com.sjtuopennetwork.shareit.setting.util.NotificationAdapter;
import com.sjtuopennetwork.shareit.setting.util.NotificationGroup;
import com.sjtuopennetwork.shareit.setting.util.NotificationItem;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import sjtu.opennet.textilepb.Model;
import sjtu.opennet.textilepb.Model.Notification;
import sjtu.opennet.hon.Textile;

public class NotificationActivity extends AppCompatActivity {

    //UI控件
    private ListView listView;
    private ExpandableListView notification_listView;

    //内存
    private List<Notification> noti_list;

    private NotificationAdapter notificationAdapter=null;
    private ArrayList<NotificationGroup> gData = null;
    private ArrayList<ArrayList<NotificationItem>> iData = null;
    private ArrayList<NotificationItem> inviteData = null;
    private ArrayList<NotificationItem> commentData = null;
    private ArrayList<NotificationItem> likeData = null;
    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        init();
    }
    private void init() {
        mContext=NotificationActivity.this;
        listView=findViewById(R.id.notification_listView);
        notification_listView=findViewById(R.id.notification_listView);
        gData = new ArrayList<NotificationGroup>();
        iData = new ArrayList<ArrayList<NotificationItem>>();
        initData();
        notificationAdapter = new NotificationAdapter(gData,iData,mContext);
        notification_listView.setAdapter(notificationAdapter);
        //为列表设置点击事件
        notification_listView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                String name=iData.get(groupPosition).get(childPosition).actor;
                String notiid=iData.get(groupPosition).get(childPosition).notiid;

                AlertDialog.Builder acceptOr=new AlertDialog.Builder(NotificationActivity.this);
                acceptOr.setTitle("邀请处理");
                acceptOr.setMessage("接受 "+name+" 的邀请吗？");
                acceptOr.setPositiveButton("接受", (dialogInterface, i1) -> {
                    try {
                        Textile.instance().notifications.acceptInvite(notiid);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                acceptOr.setNegativeButton("忽略", (dialog, which) -> {
                    try {
                        Textile.instance().notifications.ignoreInvite(notiid);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                acceptOr.show();

                return true;
            }
        });
    }
    private void initData() {
        inviteData = new ArrayList<NotificationItem>();
        commentData = new ArrayList<NotificationItem>();
        likeData = new ArrayList<NotificationItem>();
        try {
            noti_list= Textile.instance().notifications.list("",500).getItemsList();
            for(Notification n:noti_list){
                switch (n.getType()){
                    case INVITE_RECEIVED:
                        for(NotificationGroup g:gData){
                            if(g.getgName()!="邀请")
                                gData.add(new NotificationGroup("邀请"));
                        }
                        inviteData.add(new NotificationItem(n.getId(),n.getUser().getAvatar(),n.getUser().getName(), "邀请你",n.getDate().getSeconds(),true));
                        break;
                    case COMMENT_ADDED:
                        for (NotificationGroup g : gData) {
                            if (g.getgName() != "评论")
                                gData.add(new NotificationGroup("评论"));
                        }
                        commentData.add(new NotificationItem(n.getId(),n.getUser().getAvatar(),n.getUser().getName(), "进行了评论",n.getDate().getSeconds(),true));
                        break;
                    case LIKE_ADDED:
                        for (NotificationGroup g : gData) {
                            if (g.getgName() != "点赞")
                                gData.add(new NotificationGroup("点赞"));
                        }
                        likeData.add(new NotificationItem(n.getId(),n.getUser().getAvatar(),n.getUser().getName(), "点赞了",n.getDate().getSeconds(),true));
                        break;
                    default:
                        break;
                }
            }
            //测试用
//            SharedPreferences pref;
//            pref= getSharedPreferences("txtl", Context.MODE_PRIVATE);
//            String path;
//            path=pref.getString("avatarpath","null");
//            gData.add(new NotificationGroup("邀请"));
//            gData.add(new NotificationGroup("评论"));
//            gData.add(new NotificationGroup("点赞"));
//            Date d=new Date();
//            inviteData.add(new NotificationItem("1",path,"YJC", "邀请你", d.getTime(),true));
//            inviteData.add(new NotificationItem("2",path,"YJC", "邀请你", d.getTime(),true));
//            inviteData.add(new NotificationItem("3",path,"YJC", "邀请你", d.getTime(),true));

            if (inviteData != null)
                iData.add(inviteData);
            if (commentData != null)
                iData.add(commentData);
            if (likeData != null)
                iData.add(likeData);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
