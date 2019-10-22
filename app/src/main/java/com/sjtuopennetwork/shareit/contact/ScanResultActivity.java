package com.sjtuopennetwork.shareit.contact;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.sjtuopennetwork.shareit.R;

public class ScanResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_result);

        if (getIntent() != null){
            Bundle bundle = getIntent().getExtras();
            String result = bundle.getString("result");
            System.out.println("=========================================="+result);
        }

    }
}
