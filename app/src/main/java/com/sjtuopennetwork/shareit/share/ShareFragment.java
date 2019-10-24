package com.sjtuopennetwork.shareit.share;


import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.sjtuopennetwork.shareit.R;
import com.sjtuopennetwork.shareit.share.util.DialogAdapter;
import com.sjtuopennetwork.shareit.share.util.TDialog;
import com.sjtuopennetwork.shareit.util.AppdbHelper;
import com.syd.oden.circleprogressdialog.core.CircleProgressDialog;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.LinkedList;
import java.util.List;

import io.textile.textile.Textile;
import razerdp.basepopup.BasePopupWindow;

/**
 * A simple {@link Fragment} subclass.
 */
public class ShareFragment extends Fragment {

    //UI控件
    DialogAdapter dialogAdapter;  //对话列表的数据适配器
    ListView dialoglistView; //对话列表
    ImageView bt_share_menu; //右上角加号按钮
    CircleProgressDialog circleProgressDialog; //等待圆环

    //内存数据
    List<TDialog> dialogs; //对话列表数据
    boolean nodeOnline=false;

    //持久化存储
    private AppdbHelper appdbHelper;
    public SQLiteDatabase appdb;

    public ShareFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_share, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();

        initUI();

        initData();

        if(!nodeOnline){
            circleProgressDialog=new CircleProgressDialog(getActivity());
            circleProgressDialog.setText("节点启动中");
            circleProgressDialog.showDialog();
        }

        if(!EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().register(this);
        }

    }

    private void initUI(){
        dialoglistView=getActivity().findViewById(R.id.dialogs_lv);
        //右上角菜单按钮
        bt_share_menu=getActivity().findViewById(R.id.bt_share_menu);
        bt_share_menu.setOnClickListener(v -> {
            BtShareMenu btShareMenu=new BtShareMenu(getContext());
            btShareMenu.setBackgroundColor(Color.parseColor("#00000000"));

            btShareMenu.showPopupWindow(v);
        });
    }

    private void initData(){
        appdbHelper=new AppdbHelper(getActivity(),"txtl.db",null,1);
        appdb=appdbHelper.getWritableDatabase();
        dialogs=new LinkedList<>();

        //从数据库中查出对话
        Cursor cursor=appdb.query("dialogs",null,"isvisible",new String[]{"1"},null,null,"lastmsgdate asc");
        if(cursor.moveToFirst()){
            do{
                int lookid=cursor.getInt(cursor.getColumnIndex("id"));
                String threadid=cursor.getString(cursor.getColumnIndex("threadid"));
                String threadname=cursor.getString(cursor.getColumnIndex("threadname"));
                String lastmsg=cursor.getString(cursor.getColumnIndex("lastmsg"));
                long lastmsgdate=cursor.getLong(cursor.getColumnIndex("lastmsgdate"));
                boolean isread=cursor.getInt(cursor.getColumnIndex("isread"))==1;
                String imgpath=cursor.getString(cursor.getColumnIndex("imgpath"));
                boolean isSingle=cursor.getInt(cursor.getColumnIndex("issingle"))==1;

                TDialog tDialog=new TDialog(lookid,threadid,threadname,lastmsg,lastmsgdate,isread,imgpath,isSingle,true);
                dialogs.add(tDialog);
            }while (cursor.moveToNext());
        }
        cursor.close();

        //查出邀请中最近的一个，添加到头部。包括好友申请的邀请，也包括群组的邀请，不过要一下类
        io.textile.pb.View.InviteView inviteView=null;
        try {
            inviteView=Textile.instance().invites.list().getItems(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(inviteView!=null){
            TDialog invite=new TDialog(1,"1","通知",inviteView.getInviter().getName()+" 邀请",inviteView.getDate().getSeconds(),false,"tongzhi",false,true);
            dialogs.add(0,invite);
        }

        dialogAdapter=new DialogAdapter(getContext(),R.layout.item_share_dialog,dialogs);
        dialoglistView.setAdapter(dialogAdapter);

        dialoglistView.setOnItemClickListener((parent, view, position, id) -> {
            String threadid=dialogs.get(position).threadid;

            //数据库中修改为已读
            //先将对应threadlook的状态设为已读
            ContentValues v=new ContentValues();
            v.put("isread",1);
            appdb.update("dialogs",v,"threadid=?",new String[]{threadid});


            Intent it=new Intent(getActivity(), ChatActivity.class);
            it.putExtra("threadid",threadid);
            startActivity(it);
        });
    }

    //右上角菜单
    class BtShareMenu extends BasePopupWindow{
        LinearLayout create_view;
        LinearLayout qrcode_join_gorup;
        Context context;

        public BtShareMenu(Context context) {
            super(context);
            this.context=context;
            create_view=findViewById(R.id.create_group);
        }
        @Override
        public View onCreateContentView() {
            View view= createPopupById(R.layout.pop_share_add_menu);
            LinearLayout create_gp=view.findViewById(R.id.create_group);
            create_gp.setOnClickListener(v -> {

            });
            return view;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN,sticky = true)
    public void knowOnline(Integer integer){
        if(integer.intValue()==0){
            if(!nodeOnline){
                circleProgressDialog.dismiss();
                nodeOnline=true;
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void getNewMsg(){

    }

}
