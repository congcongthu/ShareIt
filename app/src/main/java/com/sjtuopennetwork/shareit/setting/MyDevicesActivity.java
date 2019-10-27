package com.sjtuopennetwork.shareit.setting;

import android.graphics.ColorSpace;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.sjtuopennetwork.shareit.R;

import java.util.List;

import io.textile.textile.Textile;

public class MyDevicesActivity extends AppCompatActivity {

    ListView lv_devices;
    List<String> list;
    private List<io.textile.pb.Model.Peer> peers_list;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_devices);

        initUI();
        initData();
        drawUI();
    }




    private void initUI() {
        lv_devices = findViewById(R.id.devices_list);
    }

    private void initData() {
        try {
            peers_list = Textile.instance().account.contact().getPeersList();//获取peers列表
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void drawUI() {
        ArrayAdapter<io.textile.pb.Model.Peer> adapter = new ArrayAdapter<io.textile.pb.Model.Peer>(
                MyDevicesActivity.this, android.R.layout.simple_list_item_1, peers_list);
        lv_devices.setAdapter(adapter);
    }



}
