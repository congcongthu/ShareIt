package com.sjtuopennetwork.shareit.setting;

import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.wildma.pictureselector.PictureSelector;
import com.sjtuopennetwork.shareit.R;

import sjtu.opennet.textilepb.Model;
import sjtu.opennet.hon.Handlers;
import sjtu.opennet.hon.Textile;

import static android.os.Build.VERSION_CODES.O;

public class PersonalInfoActivity extends AppCompatActivity {

    //UI控件
    private TextView info_name; //昵称
    private TextView info_addr; //公钥
    private TextView info_phrase; //助记词
   // private ImageView avatar_img;//头像
    com.shehuan.niv.NiceImageView avatar_img;
    private LinearLayout info_avatar_layout;   //头像板块
    private LinearLayout info_name_layout;  //昵称板块
    private LinearLayout info_addr_layout;  //公钥地址板块
    private LinearLayout info_phrase_layout;    //助记词板块
    //持久化
    private SharedPreferences pref;

    //内存
    private String myname;
    private String avatarPath;
    private String myaddr;
    private String avatarHash;
    private String phrase;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_info);
        initUI();
        initData();
        drawUI();
    }
    private void drawUI() {
        info_name.setText(myname);
        if (myname.equals("shareitlogin")){
            info_name.setText("");
        }
        info_addr.setText(myaddr);
        info_phrase.setText(phrase);
        if(!avatarPath.equals("null")){ //头像为空只可能是引导页未设置
            avatar_img.setImageBitmap(BitmapFactory.decodeFile(avatarPath));
            avatar_img.setCornerRadius(5);
        }else{
            System.out.println("=============头像路径："+avatarPath);
        }
    }
    private void initData() {
        pref=getSharedPreferences("txtl", Context.MODE_PRIVATE);

        myname=pref.getString("myname","null");
        phrase=pref.getString("phrase","null");
//        myaddr=pref.getString("myaddr","null");
        try {
            myaddr=Textile.instance().profile.get().getAddress();
        } catch (Exception e) {
            e.printStackTrace();
        }
        avatarPath=pref.getString("avatarpath","null");
        avatarHash=pref.getString("avatarhash","null");

    }

    private void initUI() {
        avatar_img = findViewById((R.id.setting_avatar_niceview));
        info_name = findViewById(R.id.info_name);
        info_addr = findViewById(R.id.info_addr);
        info_avatar_layout = findViewById(R.id.setting_personal_info_avatar);
        info_name_layout = findViewById(R.id.setting_personal_info_name);
        info_addr_layout = findViewById(R.id.setting_personal_info_address);
        info_phrase_layout=findViewById(R.id.setting_personal_info_phrase);
        info_phrase=findViewById(R.id.info_phrase);

        info_avatar_layout.setOnClickListener(v -> {
            PictureSelector.create(this, PictureSelector.SELECT_REQUEST_CODE)
                    .selectPicture();
        });

        info_name_layout.setOnClickListener(view -> {
            Intent it=new Intent(PersonalInfoActivity.this,InfoNameActivity.class);
            startActivity(it);
        });
        info_addr_layout.setOnClickListener(view -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            // 将文本内容放到系统剪贴板里。
            cm.setText(info_addr.getText());
            Toast.makeText(this,"已将地址复制到剪贴板："+info_addr.getText(),Toast.LENGTH_LONG).show();
        });
        info_phrase_layout.setOnClickListener(view -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            // 将文本内容放到系统剪贴板里。
            cm.setText(info_phrase.getText());
            Toast.makeText(this,"已将助记词复制到剪贴板："+info_phrase.getText(),Toast.LENGTH_LONG).show();
        });
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        initUI();
        initData();
        drawUI();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        /*结果回调*/
        if (requestCode == PictureSelector.SELECT_REQUEST_CODE) {
            if (data != null) {
                String picturePath = data.getStringExtra(PictureSelector.PICTURE_PATH);
                SharedPreferences.Editor editor=pref.edit();
                editor.putString("avatarpath",picturePath);
                editor.commit();
                System.out.println("=====================设置头像："+pref.getString("avatarpath","null"));
                Textile.instance().profile.setAvatar(picturePath, new Handlers.BlockHandler() {
                    @Override
                    public void onComplete(Model.Block block) {
                        System.out.println("头像设置成功！");
                    }

                    @Override
                    public void onError(Exception e) {
                        System.out.println("头像设置失败！");
                    }
                });
                avatar_img.setImageBitmap(BitmapFactory.decodeFile(picturePath));
                avatar_img.setCornerRadius(5);
            }
        }
    }


}
