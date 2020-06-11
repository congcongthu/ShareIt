package com.sjtuopennetwork.shareit.setting;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.qrlibrary.qrcode.utils.PermissionUtils;
import com.sjtuopennetwork.shareit.R;
import com.sjtuopennetwork.shareit.util.QRCodeActivity;

import java.util.ArrayList;
import java.util.List;

import sjtu.opennet.hon.Textile;
import sjtu.opennet.textilepb.Model;

public class ShadowActivity extends AppCompatActivity {
    private static final String TAG = "==============ShadowActivity";

    //UI控件
    LinearLayout shadow_scan_code;
    LinearLayout shadow_twod_code;
    TextView shadow_connect_state;
    LinearLayout shadow_retry;
//    TextView shadow_register;

    // 内存数据
    boolean isConnect = false;
    String oldAddr;

    //持久化存储
    SharedPreferences pref;

    Handler handler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "handleMessage: handler get res");
            drwaUI();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shadow);

        pref = getSharedPreferences("txtl", Context.MODE_PRIVATE);

        initUI();
    }

    @Override
    protected void onResume() {
        super.onResume();

        swarmConnectOld();
    }

    private void swarmConnectOld() {
        oldAddr = pref.getString("shadowSwarm", "null");
        if (oldAddr.equals("null")) { //初始情况
            isConnect = false;
        } else {
            new Thread() {
                @Override
                public void run() {
                    try {
                        Textile.instance().ipfs.swarmConnect(oldAddr);
                        handler.sendMessage(Message.obtain());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }.start();
        }

        Log.d(TAG, "swarmConnectOld: " + isConnect);
    }

    public void initUI() {
        shadow_scan_code = findViewById(R.id.shadow_scan_code);
        shadow_twod_code = findViewById(R.id.shadow_twod_code);
        shadow_connect_state = findViewById(R.id.shadow_connect_state);
        shadow_retry=findViewById(R.id.shadow_retry);
//        shadow_register=findViewById(R.id.shadow_register);
//        String shadow=Textile.instance().shadow();
//        if(!shadow.equals("")){
//            Log.d(TAG, "initUI: shadow not null:"+shadow);
//            shadow_register.setText(shadow);
//        }else{
//            Log.d(TAG, "initUI: shadow null");
//            shadow_register.setText("未注册影子节点");
//        }

        shadow_scan_code.setOnClickListener(v -> { //跳转到扫码连接
            PermissionUtils.getInstance().requestPermission(ShadowActivity.this);
            Intent it = new Intent(ShadowActivity.this, QRCodeActivity.class);
            startActivity(it);
        });

        shadow_retry.setOnClickListener(v-> swarmConnectOld());
    }

    public void drwaUI() {
        try {
            List<Model.SwarmPeer> peers = Textile.instance().ipfs.connectedAddresses().getItemsList();
            for (Model.SwarmPeer s : peers) {
                String[] ss = s.getAddr().split("/");
                if (ss[2].equals("192.168.3.1")) {
                    isConnect = true;
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d(TAG, "drwaUI: " + isConnect + " " + oldAddr);
        if (isConnect) { //连接成功
            Toast.makeText(this, "连接成功", Toast.LENGTH_SHORT).show();
            shadow_connect_state.setText("连接成功: " + oldAddr); //要求任何地方连接成功或失败都要同时更新两个状态。
            shadow_twod_code.setOnClickListener(v -> { //跳转到二维码
                Intent it = new Intent(ShadowActivity.this, ShadowCodeActivity.class);
                startActivity(it);
            });
        } else {
            shadow_connect_state.setText("未连接");
            shadow_twod_code.setOnClickListener(v -> { //弹出提示未连接
                Toast.makeText(this, "未连接影子节点", Toast.LENGTH_SHORT).show();
            });
        }
    }

}
