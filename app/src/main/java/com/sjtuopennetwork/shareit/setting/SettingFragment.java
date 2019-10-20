package com.sjtuopennetwork.shareit.setting;


import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.sjtuopennetwork.shareit.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class SettingFragment extends Fragment {

    //UI控件
    RelativeLayout info_layout;
    LinearLayout qrcode_layout;
    LinearLayout cafe_layout;
    LinearLayout notification_layout;



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
    }

    private void initUI() {
        info_layout=getActivity().findViewById(R.id.setting_info_layout);
        qrcode_layout=getActivity().findViewById(R.id.setting_qrcode_layout);
        cafe_layout=getActivity().findViewById(R.id.setting_cafe_layout);
        notification_layout=getActivity().findViewById(R.id.setting_notification_layout);

        info_layout.setOnClickListener(v -> {
            Intent it=new Intent(getActivity(),PersonalInfoActivity.class);
            startActivity(it);
        });
        qrcode_layout.setOnClickListener(v -> {
            Intent it=new Intent(getActivity(),PersonalQrcodeActivity.class);
            startActivity(it);
        });
        cafe_layout.setOnClickListener(v -> {
            Intent it=new Intent(getActivity(),CafeActivity.class);
            startActivity(it);
        });
        notification_layout.setOnClickListener(v -> {
            Intent it=new Intent(getActivity(),NotificationActivity.class);
            startActivity(it);
        });
    }
}
