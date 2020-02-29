package com.sjtuopennetwork.shareit.setting;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.sjtuopennetwork.shareit.R;
import com.sjtuopennetwork.shareit.login.MainActivity;
import com.sjtuopennetwork.shareit.setting.util.GetIpAddress;
import com.sjtuopennetwork.shareit.util.AppdbHelper;
import com.sjtuopennetwork.shareit.util.DBoperator;
import com.sjtuopennetwork.shareit.util.ForeGroundService;
import com.sjtuopennetwork.shareit.util.LogToHTTP;
import com.sjtuopennetwork.shareit.util.RoundImageView;

import org.apache.commons.io.FileUtils;
import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.List;

import sjtu.opennet.honvideo.FileUtil;
import sjtu.opennet.textilepb.Model;
import sjtu.opennet.hon.Textile;
import sjtu.opennet.textilepb.QueryOuterClass;

/**
 * A simple {@link Fragment} subclass.
 */
public class SettingFragment extends Fragment {

    private static final String TAG = "=====================";
    //UI控件
    RelativeLayout info_layout;
    LinearLayout qrcode_layout;
    LinearLayout cafe_layout;
    LinearLayout notification_layout;
    LinearLayout devices_layout;
    TextView tv_name;
    RoundImageView avatar_layout;
    TextView logout_layout;
    Button uploadLog;
    Button t_in;
    Button t_se;

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
        QueryOuterClass.QueryOptions options = QueryOuterClass.QueryOptions.newBuilder().build();
        try {
            Textile.instance().account.sync(options);
        } catch (Exception e) {
            e.printStackTrace();
        }

        initUI();
        initData();
        drawUI();
    }

    private void initUI() {
        info_layout = getActivity().findViewById(R.id.setting_info_layout);
        qrcode_layout = getActivity().findViewById(R.id.setting_qrcode_layout);
        //cafe_layout=getActivity().findViewById(R.id.setting_cafe_layout);
        notification_layout = getActivity().findViewById(R.id.setting_notification_layout);
        avatar_layout = getActivity().findViewById(R.id.setting_avatar_photo);
        tv_name = getActivity().findViewById(R.id.myname);
        devices_layout = getActivity().findViewById(R.id.setting_devices_layout);
        logout_layout = getActivity().findViewById(R.id.logout);

        t_in=getActivity().findViewById(R.id.test_insert_stream);
        t_se=getActivity().findViewById(R.id.test_search_stream);


        uploadLog=getActivity().findViewById(R.id.uploadlog);
        uploadLog.setOnClickListener(view -> {
            try {
                final String logPath= FileUtil.getAppExternalPath(getActivity(),"repo")+"/"+Textile.instance().profile.get().getAddress()+"/logs/textile.log";

                Handler handler=new Handler(){
                    @Override
                    public void handleMessage(Message msg) {
                        switch (msg.what){
                            case 0:
                                Toast.makeText(getActivity(), "上传成功", Toast.LENGTH_SHORT).show(); break;
                            case 1:
                                Toast.makeText(getActivity(), "上传失败", Toast.LENGTH_SHORT).show(); break;
                        }
                    }
                };

                new Thread(){
                    @Override
                    public void run() {
                        Log.d(TAG, "initUI: log路径："+logPath);
                        String response=LogToHTTP.uploadLog(logPath);
                        Log.d(TAG, "run: 上传结果："+response);
                        if(response.equals("success")){
                            Message msg=new Message(); msg.what=0;
                            handler.sendMessage(msg);
                        }else{
                            Message msg=new Message(); msg.what=1;
                            handler.sendMessage(msg);
                        }
                    }
                }.start();

            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        info_layout.setOnClickListener(v -> {
            Intent it = new Intent(getActivity(), PersonalInfoActivity.class);
            startActivity(it);
        });
        qrcode_layout.setOnClickListener(v -> {
            Intent it = new Intent(getActivity(), PersonalQrcodeActivity.class);
            startActivity(it);
        });
//        cafe_layout.setOnClickListener(v -> {
//            Intent it=new Intent(getActivity(),CafeActivity.class);
//            startActivity(it);
//        });
        notification_layout.setOnClickListener(v -> {
            Intent it = new Intent(getActivity(), NotificationActivity.class);
            startActivity(it);
        });
        devices_layout.setOnClickListener(v -> {
            Intent it = new Intent(getActivity(), MyDevicesActivity.class);
            startActivity(it);
        });
        logout_layout.setOnClickListener(v -> {
            logout();
        });
    }

    private void logout() {
        final AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
        dialog.setTitle("退出登录");
        dialog.setMessage("确定要退出登录吗？");
        dialog.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                SharedPreferences.Editor editor = pref.edit();
                editor.putBoolean("isLogin", false);
                editor.commit();

                //销毁Texile及数据库
                EventBus.getDefault().post(new Integer(943));

                AppdbHelper.setNull();

                Intent it = new Intent(getActivity(), MainActivity.class);
                startActivity(it);

                getActivity().finish();
            }
        });
        dialog.setNegativeButton("关闭", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });
        AlertDialog d = dialog.create();
        d.show();
    }

    private List<Model.Thread> threads;
    private Model.ThreadList threadList;

    private void initData() {
        pref = getActivity().getSharedPreferences("txtl", Context.MODE_PRIVATE);

        myname = pref.getString("myname", "null");
        imagePath = pref.getString("avatarpath", "null");
        System.out.println("============头像路径：" + imagePath);

        try {
            threadList = Textile.instance().threads.list();
            threads = threadList.getItemsList();
            for (Model.Thread t : threads) {
                if (t.getName().equals("mydevice1219")) {//找到mydevice1219这个thread
                    Context context = this.getActivity().getApplicationContext();
                    String IP= GetIpAddress.getIPAddress(context);
                    String addr="/ip4/"+IP+"/tcp/40601/ipfs/"+ Textile.instance().ipfs.peerId();
                    System.out.println("=========="+addr);

                    String androidId = Settings.System.getString(context.getContentResolver(),
                            Settings.Secure.ANDROID_ID);

                    String device = "厂商：" + android.os.Build.BRAND + "  型号：" + Build.MODEL + "#" + androidId + "%" + addr;//获取设备厂商、型号、系统id
                    Boolean flag = true;
                    sjtu.opennet.textilepb.View.TextList textList = Textile.instance().messages.list("", 100, t.getId());
                    for (int i = 0; i < textList.getItemsCount(); i++) {
                        if (textList.getItems(i).getBody().equals(device)) {
                            flag = false;
                            break;
                        }
                    }
                    if (flag == true) {
                        Textile.instance().messages.add(t.getId(), device);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void drawUI() {
        tv_name.setText(myname);

        if (!imagePath.equals("null") && !imagePath.equals("")) {
            avatar_layout.setImageBitmap(BitmapFactory.decodeFile(imagePath));
            //avatar_layout.setCornerRadius(10);
            System.out.println("======显示圆角头像");
        }
    }

}