package com.sjtuopennetwork.shareit.contact;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.shehuan.niv.NiceImageView;
import com.sjtuopennetwork.shareit.R;
import com.sjtuopennetwork.shareit.share.ChatActivity;
import com.sjtuopennetwork.shareit.util.AppdbHelper;
import com.sjtuopennetwork.shareit.util.DBoperator;
import com.sjtuopennetwork.shareit.util.FileUtil;

import java.util.List;

import sjtu.opennet.textilepb.Model;
import sjtu.opennet.hon.Handlers;
import sjtu.opennet.hon.Textile;

public class ContactInfoActivity extends AppCompatActivity {

    //UI控件
    TextView contact_name;
    TextView contact_addr;
    NiceImageView contact_avatar;
    TextView contact_del;
    TextView contact_send;

    //内存数据
    String address;
    Model.Contact contact;
    Model.Peer peer;
    String threadid;

    //持久化存储
    public SQLiteDatabase appdb;
    public SharedPreferences pref;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_info);

        address=getIntent().getStringExtra("address");
        try {
            contact= Textile.instance().contacts.get(address);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("======用户详情"+contact.getName()+" ");

        initUI();

        initData();
    }

    private void initUI() {
        contact_name=findViewById(R.id.contact_info_name);
        contact_addr=findViewById(R.id.contact_info_addr);
        contact_del=findViewById(R.id.contact_info_del);
        contact_send=findViewById(R.id.contact_info_send);

        contact_name.setText(contact.getName());
        contact_addr.setText("公钥："+contact.getAddress().substring(0,10)+"...");

        contact_avatar=findViewById(R.id.contact_info_avatar);
    }
    private void initData(){

        pref=getSharedPreferences("txtl", Context.MODE_PRIVATE);
        appdb= AppdbHelper.getInstance(this,pref.getString("loginAccount","")).getWritableDatabase();

        //从address得到peerid
        try {
            List<Model.Peer> peers=Textile.instance().threads.peers(threadid).getItemsList();
            for(Model.Peer p:peers){
                if(p.getAddress().equals(address)){
                    peer=p;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


        //显示头像
        String avatarPath= FileUtil.getFilePath(contact.getAvatar());
        if(avatarPath.equals("null")){ //如果没有存储过这个头像文件
            Textile.instance().ipfs.dataAtPath("/ipfs/" + contact.getAvatar() + "/0/small/content", new Handlers.DataHandler() {
                @Override
                public void onComplete(byte[] data, String media) {
                    String newPath=FileUtil.storeFile(data,contact.getAvatar());
                    contact_avatar.setImageBitmap(BitmapFactory.decodeFile(newPath));
                }
                @Override
                public void onError(Exception e) {
                }
            });
        }else{ //如果已经存储过这个头像
            contact_avatar.setImageBitmap(BitmapFactory.decodeFile(avatarPath));
        }

        List<Model.Thread> devicethreads= null;
        try {
            devicethreads = Textile.instance().threads.list().getItemsList();
        } catch (Exception e) {
            e.printStackTrace();
        }
        for(Model.Thread t:devicethreads){
            if(t.getWhitelistCount()==2){
                if(t.getWhitelist(0).equals(address) || t.getWhitelist(1).equals(address)){
                    threadid=t.getId();
                    System.out.println("================threadID："+threadid);
                }
            }
        }

        contact_del.setOnClickListener(v -> {
            //删除好友,removeThread，并在自己的数据库中删除记录
            try {
                Textile.instance().threads.removePeer(threadid, peer.getId()); //将对方移出peer
                Textile.instance().threads.remove(threadid); //将自己的thread删除
            } catch (Exception e) {
                e.printStackTrace();
            }
            DBoperator.deleteDialogByThreadID(appdb,threadid); //将自己的对话数据库中的记录删除

            finish();
        });

        contact_send.setOnClickListener(v -> {
            //发消息，直接找到threadid，跳转到相应的ChatActivity就行
            Intent it=new Intent(this, ChatActivity.class);
            it.putExtra("threadid",threadid);
            startActivity(it);
        });
    }
}
