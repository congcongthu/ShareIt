package com.sjtuopennetwork.shareit.share;


import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;

import com.example.qrlibrary.qrcode.utils.PermissionUtils;
import com.sjtuopennetwork.shareit.R;
import com.sjtuopennetwork.shareit.contact.util.ContactUtil;
import com.sjtuopennetwork.shareit.setting.NotificationActivity;
import com.sjtuopennetwork.shareit.share.util.DialogAdapter;
import com.sjtuopennetwork.shareit.share.util.TDialog;
import com.sjtuopennetwork.shareit.util.AppdbHelper;
import com.sjtuopennetwork.shareit.util.DBoperator;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.LinkedList;
import java.util.List;

import sjtu.opennet.textilepb.Model;
import sjtu.opennet.hon.Textile;

/**
 * A simple {@link Fragment} subclass.
 */
public class ShareFragment extends Fragment {

    //UI控件
    DialogAdapter dialogAdapter;  //对话列表的数据适配器
    ListView dialoglistView; //对话列表
    ImageView bt_share_menu; //右上角加号按钮
    ImageView qrcodeJoinGroup; //扫码加群

    //内存数据
    List<TDialog> dialogs; //对话列表数据

    //持久化存储
    public SQLiteDatabase appdb;
    public SharedPreferences pref;

    public ShareFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        System.out.println("==============fagment生命周期：conCreateView");
        return inflater.inflate(R.layout.fragment_share, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();

        initUI();

        initData();


        if(!EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().register(this);
        }

    }

    private void initUI(){
        dialoglistView=getActivity().findViewById(R.id.dialogs_lv);
        //右上角菜单按钮
        bt_share_menu=getActivity().findViewById(R.id.bt_share_add);
        bt_share_menu.setOnClickListener(v -> {

            //直接跳转一个新的Activity
            Intent it=new Intent(getActivity(),NewGroupActivity.class);
            startActivity(it);
        });

        qrcodeJoinGroup=getActivity().findViewById(R.id.bt_share_scan);
        qrcodeJoinGroup.setOnClickListener(v -> {

            PermissionUtils.getInstance().requestPermission(getActivity());
            Intent it=new Intent(getActivity(),GroupQRCodeActivity.class);
            startActivity(it);
        });
    }

    private void initData(){
        pref=getActivity().getSharedPreferences("txtl",Context.MODE_PRIVATE);
        appdb=AppdbHelper.getInstance(getContext(),pref.getString("loginAccount","")).getWritableDatabase();
        dialogs=new LinkedList<>();

        //从数据库中查出对话
        dialogs= DBoperator.queryAllDIalogs(appdb);
        System.out.println("================数据库中dialog数："+dialogs.size());

        //查出邀请中最近的一个，添加到头部。
        int gpinvite=0;
        sjtu.opennet.textilepb.View.InviteView lastInvite=null;
        try {
//            List<Model.Peer> friends= ContactUtil.getFriendList();
            List<sjtu.opennet.textilepb.View.InviteView> invites = Textile.instance().invites.list().getItemsList();
            for(sjtu.opennet.textilepb.View.InviteView v:invites){ //遍历所有的邀请
                if(!v.getName().equals("FriendThread1219")){ //只要群组名不等于这个那就是好友邀请
                    gpinvite++;
                    lastInvite=v;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("=============邀请："+gpinvite);
        if(gpinvite>0){ //如果有群组邀请就要显示出来
            TDialog noti=new TDialog(1,"","通知",lastInvite.getInviter().getName()+" 邀请你",
                    lastInvite.getDate().getSeconds(),false,"tongzhi",true,true);
            dialogs.add(0,noti);
        }

        dialogAdapter=new DialogAdapter(getContext(),R.layout.item_share_dialog,dialogs);
        dialogAdapter.notifyDataSetChanged();
        dialoglistView.setAdapter(dialogAdapter);

        dialoglistView.setOnItemClickListener((parent, view, position, id) -> {
            if(dialogs.get(position).imgpath.equals("tongzhi")){ //如果是通知，就跳转过去
                Intent intent=new Intent(getActivity(), NotificationActivity.class);
                startActivity(intent);
            }else{ //如果是对话就进入聊天
                String threadid=dialogs.get(position).threadid;
                //数据库中修改为已读
                //先将对应threadlook的状态设为已读
                ContentValues v=new ContentValues();
                v.put("isread",1);
                appdb.update("dialogs",v,"threadid=?",new String[]{threadid});

                Intent it=new Intent(getActivity(), ChatActivity.class);
                it.putExtra("threadid",threadid);
                startActivity(it);
            }
        });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void getNewMsg(TDialog tDialog){ //获取到新的消息后要更新显示
        for(TDialog t:dialogs){
            if(t.threadid.equals(tDialog.threadid)){
                dialogs.remove(t);
                dialogs.add(0,tDialog);
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
