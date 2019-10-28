package com.sjtuopennetwork.shareit.setting;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.sjtuopennetwork.shareit.R;

import io.textile.textile.Textile;

public class InfoNameActivity extends AppCompatActivity {

    private EditText et_name;//昵称
    private Button bt_confirm;
    //持久化
    private SharedPreferences pref;

    private String myname;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info_name);

        initUI();
        initData();
        drawUI();
    }

    private void drawUI() {
        et_name.setText(myname);
        bt_confirm.setOnClickListener(View -> {
            myname = et_name.getText().toString();
            SharedPreferences.Editor editor = pref.edit();
            editor.putString("myname", myname);
            editor.apply();
            try {
                System.out.println("=========新昵称："+myname);
                Textile.instance().profile.setName(myname);
                System.out.println("============设置成功："+myname);
            } catch (Exception e) {
                e.printStackTrace();
            }
            finish();
        });
    }

    private void initData() {
        pref=getSharedPreferences("txtl", Context.MODE_PRIVATE);
        myname=pref.getString("myname","null");

    }

    private void initUI() {
        et_name = findViewById(R.id.name);
        bt_confirm = findViewById(R.id.confirm);

    }
}
