package com.sjtuopennetwork.shareit.setting;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.sjtuopennetwork.shareit.R;
import com.sjtuopennetwork.shareit.util.ShareUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import mobile.HlogHandler;
import sjtu.opennet.hon.Textile;
import sjtu.opennet.textilepb.View;

public class LogActivity extends AppCompatActivity {

    TextView showLog;
    Button bt;

    String log="";
    String endLog;
    String pid;

    DateFormat dfd=new SimpleDateFormat("MM-dd-HH:mm");

    String dir= ShareUtil.getLogDir();

    Handler handler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case 0:
                    showLog.setText(log); break;
                case 1:
                    Toast.makeText(LogActivity.this, "log结束", Toast.LENGTH_SHORT).show();
                    break;
            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);

        showLog=findViewById(R.id.showlog);

        try {
            pid=Textile.instance().profile.get().getId();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Textile.instance().getLog(new HlogHandler() {
            @Override
            public void handleLog(String s) {
                log+=(s+"\n\n");
                Message msg=new Message();
                msg.what=0;
                handler.sendMessage(msg);
            }

            @Override
            public void logEnd() {
                Message msg=new Message();
                msg.what=1;
                handler.sendMessage(msg);
            }
        });

        bt=findViewById(R.id.save_txtl_log);
        bt.setOnClickListener(v->{
            String logDate=dfd.format(System.currentTimeMillis());
            File f=new File(dir+"/"+pid+"_"+logDate+".txt");
            try {
                FileWriter fr=new FileWriter(f);
                fr.write(log);
                fr.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Toast.makeText(this, "保存成功："+f.getAbsolutePath(), Toast.LENGTH_SHORT).show();
        });
    }
}
