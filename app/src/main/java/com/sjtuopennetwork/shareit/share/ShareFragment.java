package com.sjtuopennetwork.shareit.share;


import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.Pair;
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
import com.sjtuopennetwork.shareit.share.util.PreloadVideoThread;
import com.sjtuopennetwork.shareit.share.util.TDialog;
import com.sjtuopennetwork.shareit.share.util.TMsg;
import com.sjtuopennetwork.shareit.util.AppdbHelper;
import com.sjtuopennetwork.shareit.util.DBoperator;
import com.sjtuopennetwork.shareit.util.QRCodeActivity;

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

    private static final String TAG = "====================";

    //UI控件
    DialogAdapter dialogAdapter;  //对话列表的数据适配器
    ListView dialoglistView; //对话列表
    ImageView bt_share_menu; //右上角加号按钮
    ImageView qrcodeJoinGroup; //扫码加群

    //内存数据
    List<TDialog> dialogs; //对话列表数据
    List<PreloadVideoThread> preloadVideoThreadList;

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
        Log.d(TAG, "onCreateView: ShareFragment调用了onCreateView");
        return inflater.inflate(R.layout.fragment_share, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();

        preloadVideoThreadList=new LinkedList<>();

        Log.d(TAG, "onStart: ShareFragment的onSTart方法调用");

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

            Intent it=new Intent(getActivity(), QRCodeActivity.class);
            startActivity(it);
        });
    }

    private void initData(){
        pref=getActivity().getSharedPreferences("txtl",Context.MODE_PRIVATE);
        appdb=AppdbHelper.getInstance(getActivity().getApplicationContext(),pref.getString("loginAccount","")).getWritableDatabase();
        dialogs=new LinkedList<>();

        //从数据库中查出对话
        dialogs= DBoperator.queryAllDIalogs(appdb);
        Log.d(TAG, "initData: 对话数："+dialogs.size());

        //查出邀请中最近的一个，添加到头部。
        int gpinvite=0;
        sjtu.opennet.textilepb.View.InviteView lastInvite=null;
        try {
            if(Textile.instance().invites!=null){
                List<sjtu.opennet.textilepb.View.InviteView> invites = Textile.instance().invites.list().getItemsList();
                for(sjtu.opennet.textilepb.View.InviteView v:invites){ //遍历所有的邀请
                    if(!v.getName().equals("FriendThread1219")){ //只要群组名不等于这个那就是好友邀请
                        gpinvite++;
                        lastInvite=v;
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        if(gpinvite>0){ //如果有群组邀请就要显示出来
            TDialog noti=new TDialog(1,"","通知",lastInvite.getInviter().getName()+" 邀请你",
                    lastInvite.getDate().getSeconds(),false,"tongzhi",true,true);
            dialogs.add(0,noti);
        }

        dialogAdapter=new DialogAdapter(getContext(),R.layout.item_share_dialog,dialogs);
        dialogAdapter.notifyDataSetChanged();
        dialoglistView.setAdapter(dialogAdapter);

        dialoglistView.setOnItemClickListener((parent, view, position, id) -> {
            if(dialogs.get(position).imgpath.equals("tongzhi")){ //如果是通知，就跳转到通知
                Intent intent=new Intent(getActivity(), NotificationActivity.class);
                startActivity(intent);
            }else{ //如果是对话就进入聊天
                String threadid=dialogs.get(position).threadid;
                //数据库中修改为已读
                DBoperator.changeDialogRead(appdb,threadid,1);

                Intent it=new Intent(getActivity(), ChatActivity.class);
                it.putExtra("threadid",threadid);
                startActivity(it);
            }
        });
    }

//    @Subscribe(threadMode = ThreadMode.MAIN)
//    public void getApplication(Integer integer){
//        if(integer==7384){
//            //直接同意并进入群聊
//        }
//    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void getNewMsg(TDialog tDialog){ //获取到新的消息后要更新显示
        Log.d(TAG, "getNewMsg: 对话更新："+tDialog.threadname);

        if(!tDialog.threadname.equals("通知")){
            //找到对应的那一个，将其删除，并在头部插入，如果没有找到就直接在头部插入。
            for(int i=0;i<dialogs.size();i++){
                if(dialogs.get(i).threadid.equals(tDialog.threadid)){
                    dialogs.remove(i);
                }
            }
        }

        dialogs.add(0,tDialog);
        dialogAdapter.notifyDataSetChanged();
        dialoglistView.setSelection(0);
    }

    @Override
    public void onStop() {
        super.onStop();
        if(EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().unregister(this);
        }
    }
}
