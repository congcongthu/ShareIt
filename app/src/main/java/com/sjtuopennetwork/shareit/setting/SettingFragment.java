package com.sjtuopennetwork.shareit.setting;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.sjtuopennetwork.shareit.R;
import com.sjtuopennetwork.shareit.contact.SearchContactActivity;
import com.sjtuopennetwork.shareit.contact.util.ContactUtil;
import com.sjtuopennetwork.shareit.login.MainActivity;
import com.sjtuopennetwork.shareit.setting.util.GetIpAddress;
import com.sjtuopennetwork.shareit.util.DBHelper;
import com.sjtuopennetwork.shareit.util.RoundImageView;
import com.sjtuopennetwork.shareit.util.ShareUtil;

import org.greenrobot.eventbus.EventBus;

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
    LinearLayout notification_layout;
    LinearLayout devices_layout;
    LinearLayout shadow_layout;
    TextView tv_name;
    RoundImageView avatar_layout;
    TextView logout_layout;
//    Button uploadLog;
    Button setDegree;
    EditText degree;
    TextView show_worker;

    //持久化
    private SharedPreferences pref;

    //内存
    private String username;
    private String useravatar;

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
        notification_layout = getActivity().findViewById(R.id.setting_notification_layout);
        avatar_layout = getActivity().findViewById(R.id.setting_avatar_photo);
        tv_name = getActivity().findViewById(R.id.myname);
        devices_layout = getActivity().findViewById(R.id.setting_devices_layout);
        logout_layout = getActivity().findViewById(R.id.logout);
        shadow_layout=getActivity().findViewById(R.id.shadow_layout);
        shadow_layout.setOnClickListener(v->{
            Intent itToShadow=new Intent(getActivity(),ShadowActivity.class);
            getActivity().startActivity(itToShadow);
        });

        setDegree=getActivity().findViewById(R.id.setDegree);
        degree=getActivity().findViewById(R.id.degree);
        show_worker=getActivity().findViewById(R.id.show_worker);
        long tmpworker=Textile.instance().streams.getWorker();
        show_worker.setText("(当前:"+tmpworker+")");
        setDegree.setOnClickListener(view -> {
            int deg=Integer.parseInt(degree.getText().toString());
            Log.d(TAG, "initUI: degree: "+deg);
            Textile.instance().streams.setDegree(deg);
            long tmpworker1=Textile.instance().streams.getWorker();
            show_worker.setText("(当前:"+tmpworker1+")");
            Toast.makeText(getActivity(), "设置成功", Toast.LENGTH_SHORT).show();
        });

//        uploadLog=getActivity().findViewById(R.id.uploadlog);
//        uploadLog.setOnClickListener(view -> {
////            try {
////                final String logPath= FileUtil.getAppExternalPath(getActivity(),"repo")+"/"+Textile.instance().profile.get().getAddress()+"/logs/textile.log";
////
////                Handler handler=new Handler(){
////                    @Override
////                    public void handleMessage(Message msg) {
////                        switch (msg.what){
////                            case 0:
////                                Toast.makeText(getActivity(), "上传成功", Toast.LENGTH_SHORT).show(); break;
////                            case 1:
////                                Toast.makeText(getActivity(), "上传失败", Toast.LENGTH_SHORT).show(); break;
////                        }
////                    }
////                };
////
////                new Thread(){
////                    @Override
////                    public void run() {
////                        Log.d(TAG, "initUI: log路径："+logPath);
////                        String response=LogToHTTP.uploadLog(logPath);
////                        Log.d(TAG, "run: 上传结果："+response);
////                        if(response.equals("success")){
////                            Message msg=new Message(); msg.what=0;
////                            handler.sendMessage(msg);
////                        }else{
////                            Message msg=new Message(); msg.what=1;
////                            handler.sendMessage(msg);
////                        }
////                    }
////                }.start();
////
////            } catch (Exception e) {
////                e.printStackTrace();
////            }
//        });

        info_layout.setOnClickListener(v -> {
            Intent it = new Intent(getActivity(), PersonalInfoActivity.class);
            startActivity(it);
        });
        qrcode_layout.setOnClickListener(v -> {
            Intent it = new Intent(getActivity(), PersonalQrcodeActivity.class);
            startActivity(it);
        });
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

                DBHelper.setNull();

                Intent it = new Intent(getActivity(), MainActivity.class);
                startActivity(it);

                getActivity().finish();
            }
        });
        dialog.setNegativeButton("取消", (dialogInterface, i) -> { });
        AlertDialog d = dialog.create();
        d.show();
    }

    private void initData() {
        pref = getActivity().getSharedPreferences("txtl", Context.MODE_PRIVATE);

        username=ShareUtil.getMyName();
        useravatar=ShareUtil.getMyAvatar();
        Log.d(TAG, "initData: name avatar: "+username+" "+useravatar);

        //得到设备信息
        try {
            Model.Thread t= ShareUtil.getThreadByName("mydevice1219");
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void drawUI() {
        tv_name.setText(username);
        ShareUtil.setImageView(getContext(),avatar_layout,useravatar,0);
    }

}