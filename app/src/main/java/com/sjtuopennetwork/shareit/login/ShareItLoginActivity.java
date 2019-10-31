package com.sjtuopennetwork.shareit.login;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import com.sjtuopennetwork.shareit.R;
import com.sjtuopennetwork.shareit.share.HomeActivity;

public class ShareItLoginActivity extends AppCompatActivity {

    //UI控件
    EditText editText;
    Button logInWithPhrase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_it_login);

        editText=findViewById(R.id.edt_phrase);
        logInWithPhrase=findViewById(R.id.shareItLoginWithPhrase);

        logInWithPhrase.setOnClickListener(v -> {
            String phrase=editText.getText().toString();

            Intent toHomeActivity=new Intent(ShareItLoginActivity.this, HomeActivity.class);
            toHomeActivity.putExtra("login",3); //shareit登录
            toHomeActivity.putExtra("phrase",phrase);
            startActivity(toHomeActivity);
            finish();
        });
    }
}
