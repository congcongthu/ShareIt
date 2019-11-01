package com.sjtuopennetwork.shareit.setting;

import android.graphics.ColorSpace;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.sjtuopennetwork.shareit.R;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import sjtu.opennet.hon.Textile;
import sjtu.opennet.textilepb.Model;

public class MyDevicesActivity extends AppCompatActivity {

    ListView lv_devices;
    List<String> list;
    private List<sjtu.opennet.textilepb.Model.Peer> peers_list;
    private List<String> device_list;
    private List<Model.Thread> threads;
    sjtu.opennet.textilepb.View.TextList textList;

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
//        try {
//            peers_list = Textile.instance().account.contact().getPeersList();//获取peers列表
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        try {
            threads=Textile.instance().threads.list().getItemsList();
            device_list=new ArrayList<>();
            for(Model.Thread t:threads) {
                if (t.getName().equals("mydevice1219")) {//找到mydevice1219这个thread
                    textList = Textile.instance().messages.list("", 100, t.getId());
                    for(int i=0;i<textList.getItemsCount();i++){
                        device_list.add(textList.getItems(i).getBody());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void drawUI() {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                MyDevicesActivity.this, android.R.layout.simple_list_item_1, device_list);
        lv_devices.setAdapter(adapter);
    }



}
