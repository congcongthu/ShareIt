package com.sjtuopennetwork.shareit.setting;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.sjtuopennetwork.shareit.R;
import com.sjtuopennetwork.shareit.login.MainActivity;
import com.sjtuopennetwork.shareit.util.FileUtil;

import java.util.List;

import sjtu.opennet.textilepb.Model;
import sjtu.opennet.hon.Handlers;
import sjtu.opennet.hon.Textile;

/**
 * A simple {@link Fragment} subclass.
 */
public class SettingFragment extends Fragment {

    //UI控件
    RelativeLayout info_layout;
    LinearLayout qrcode_layout;
    LinearLayout cafe_layout;
    LinearLayout notification_layout;
    LinearLayout devices_layout;
    TextView tv_name;
    com.shehuan.niv.NiceImageView avatar_layout;
    TextView logout_layout;
    //持久化
    private SharedPreferences pref;
    //内存
    private String myname;
    private String imagePath;

    public SettingFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_setting, container, false);

    }

    @Override
    public void onStart() {
        super.onStart();

        initUI();
        initData();
        drawUI();
    }

    private void initUI() {
        info_layout=getActivity().findViewById(R.id.setting_info_layout);
        qrcode_layout=getActivity().findViewById(R.id.setting_qrcode_layout);
        //cafe_layout=getActivity().findViewById(R.id.setting_cafe_layout);
        notification_layout=getActivity().findViewById(R.id.setting_notification_layout);
        avatar_layout=getActivity().findViewById(R.id.setting_avatar_photo);
        tv_name=getActivity().findViewById(R.id.myname);
        devices_layout=getActivity().findViewById(R.id.setting_devices_layout);
        logout_layout=getActivity().findViewById(R.id.logout);

        info_layout.setOnClickListener(v -> {
            Intent it=new Intent(getActivity(),PersonalInfoActivity.class);
            startActivity(it);
        });
        qrcode_layout.setOnClickListener(v -> {
            Intent it=new Intent(getActivity(),PersonalQrcodeActivity.class);
            startActivity(it);
        });
//        cafe_layout.setOnClickListener(v -> {
//            Intent it=new Intent(getActivity(),CafeActivity.class);
//            startActivity(it);
//        });
        notification_layout.setOnClickListener(v -> {
            Intent it=new Intent(getActivity(),NotificationActivity.class);
            startActivity(it);
        });
        devices_layout.setOnClickListener(v -> {
            Intent it=new Intent(getActivity(),MyDevicesActivity.class);
            startActivity(it);
        });
        logout_layout.setOnClickListener(v -> {
            logout();
        });
    }

    private  void logout()
    {
        final AlertDialog.Builder dialog=new AlertDialog.Builder(getActivity());
        dialog.setTitle("退出登录");
        dialog.setMessage("确定要退出登录吗？");
        dialog.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                SharedPreferences.Editor editor=pref.edit();
                editor.putBoolean("isLogin",false);
                editor.commit();

                Textile.instance().destroy();

                Intent it=new Intent(getActivity(), MainActivity.class);
                startActivity(it);
            }
        });
        dialog.setNegativeButton("关闭", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });
        AlertDialog d=dialog.create();
        d.show();
    }
    private List<Model.Thread> threads;
    private Model.ThreadList threadList;
    private void initData() {
        pref= getActivity().getSharedPreferences("txtl", Context.MODE_PRIVATE);

        myname=pref.getString("myname","null");
        imagePath=pref.getString("avatarpath","null");
        System.out.println("============头像路径："+imagePath);

        try {
            threadList=Textile.instance().threads.list();
            threads=threadList.getItemsList();
            for(Model.Thread t:threads){
                if (t.getName().equals("mydevice1219")) {//找到mydevice1219这个thread
                    String device = android.os.Build.BRAND + " " + Build.MODEL;//获取设备厂商和型号
                    Boolean flag = true;
                    sjtu.opennet.textilepb.View.TextList textList=Textile.instance().messages.list("",100,t.getId());
                    for (int i=0;i<textList.getItemsCount();i++) {
                        if (textList.getItems(i).getBody().equals(device)){
                            flag = false;
                            break;
                        }
                    }
                    if (flag == true) {
                        Textile.instance().messages.add(t.getId(),device);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void drawUI() {
        tv_name.setText(myname);
        if(myname.equals("shareitlogin")){ //表明是shareit助记词登录的
            try {
                myname=Textile.instance().profile.name();
                final String avatarHash=Textile.instance().profile.avatar();
                imagePath= FileUtil.getFilePath(avatarHash);
                if(imagePath.equals("null")){ //如果没有存储过
                    String getAvatar="/ipfs/" + Textile.instance().profile.avatar() + "/0/small/content";
                    System.out.println("=========getAvatar:"+getAvatar);
                    Textile.instance().ipfs.dataAtPath(getAvatar, new Handlers.DataHandler() {
                        @Override
                        public void onComplete(byte[] data, String media) {
                            String storePath=FileUtil.storeFile(data,avatarHash);
                            avatar_layout.setImageBitmap(BitmapFactory.decodeFile(storePath));
                        }
                        @Override
                        public void onError(Exception e) {
                        }
                    });
                }else{
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if(!imagePath.equals("null")){
            avatar_layout.setImageBitmap(BitmapFactory.decodeFile(imagePath));
            avatar_layout.setCornerRadius(10);
        }
        try {
            if(Textile.instance().profile.name().equals("")){
                Textile.instance().profile.setName(myname);
            }
            if(Textile.instance().profile.avatar().equals("")){
                Textile.instance().profile.setAvatar(imagePath, new Handlers.BlockHandler() {
                    @Override
                    public void onComplete(Model.Block block) {
                        System.out.println("头像设置成功！");
                    }

                    @Override
                    public void onError(Exception e) {
                        System.out.println("头像设置失败！");
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
