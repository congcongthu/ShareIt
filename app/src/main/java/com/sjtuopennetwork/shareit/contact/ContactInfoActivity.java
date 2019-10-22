package com.sjtuopennetwork.shareit.contact;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.sjtuopennetwork.shareit.R;

import io.textile.pb.Model;
import io.textile.textile.Textile;

public class ContactInfoActivity extends AppCompatActivity {

    //UI控件
    TextView title_name;

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
    }

    private void initUI() {
        title_name=findViewById(R.id.title_contact_info);
        title_name.setText(contact.getName());



    }

}
