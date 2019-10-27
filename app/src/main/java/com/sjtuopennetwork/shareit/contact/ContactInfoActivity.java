package com.sjtuopennetwork.shareit.contact;

import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.shehuan.niv.NiceImageView;
import com.sjtuopennetwork.shareit.R;
import com.sjtuopennetwork.shareit.util.FileUtil;

import io.textile.pb.Model;
import io.textile.textile.Handlers;
import io.textile.textile.Textile;

public class ContactInfoActivity extends AppCompatActivity {

    //UI控件
    TextView contact_name;
    TextView contact_addr;
    NiceImageView contact_avatar;

    //内存数据
    String address;
    Model.Contact contact;

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

        contact_name.setText(contact.getName());
        contact_addr.setText(contact.getAddress());

        contact_avatar=findViewById(R.id.contact_info_avatar);
    }
    private void initData(){
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
    }
}
