package com.sjtuopennetwork.shareit.login;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.sjtuopennetwork.shareit.R;
import com.sjtuopennetwork.shareit.share.HomeActivity;

import java.io.File;

import sjtu.opennet.hon.Textile;
import sjtu.opennet.textilepb.Mobile;

public class ShareItLoginActivity extends AppCompatActivity {

    //UI控件
    EditText editText;
    Button logInWithPhrase;

    //持久化存储
    SharedPreferences pref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_it_login);

        pref=getSharedPreferences("txtl",MODE_PRIVATE);

        editText=findViewById(R.id.edt_phrase);
        logInWithPhrase=findViewById(R.id.shareItLoginWithPhrase);

        logInWithPhrase.setOnClickListener(v -> {
            String phrase=editText.getText().toString();
            //写入助记词
            SharedPreferences.Editor editor=pref.edit();
            editor.putString("phrase",phrase);
            editor.putString("myname","");
            editor.putString("avatarpath","null"); //先预设为这个
            editor.commit();

            boolean loginWrong=false;
            String loginAccount="";
            String repoPath="";
            final File filesDir = this.getFilesDir();
            try {
                Mobile.MobileWalletAccount m= Textile.walletAccountAt(phrase,Textile.WALLET_ACCOUNT_INDEX,Textile.WALLET_PASSPHRASE);
                loginAccount=m.getAddress();
                final File repo1 = new File(filesDir, loginAccount);
                repoPath = repo1.getAbsolutePath();
                if (!Textile.isInitialized(repoPath)){
                    Textile.initialize(repoPath,m.getSeed() , true, false);
                }
            } catch (Exception e) {
                loginWrong=true;
                System.out.println("===============助记词错误");
            }

            if(loginWrong){ //如果登录出了问题
                Toast.makeText(this,"助记词错误",Toast.LENGTH_SHORT).show();
                editText.setText("");
            }else {
                System.out.println("================德尔内里股："+loginAccount);
                editor.putString("loginAccount",loginAccount);
                editor.commit();

                Intent toHomeActivity=new Intent(ShareItLoginActivity.this, HomeActivity.class);
                toHomeActivity.putExtra("login",3); //shareit登录
                toHomeActivity.putExtra("repopath",repoPath);
                startActivity(toHomeActivity);
                finish();
            }
        });
    }
}
