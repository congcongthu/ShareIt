package com.sjtuopennetwork.shareit.share;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.sjtuopennetwork.shareit.R;
import com.sjtuopennetwork.shareit.share.util.TRecord;
import com.sjtuopennetwork.shareit.util.DBHelper;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import sjtu.opennet.stream.util.FileUtil;

public class FileTransActivity extends AppCompatActivity {

    private static final String TAG = "===================FileTransActivity";

    private TextView rtt;
    private TextView getAvr;
    private TextView sendDuration;
    private ListView recordsLv;
    private Button saveLog;

    private String fileCid;
    private LinkedList<TRecord> records;
    SharedPreferences pref;
    String loginAccount;
    RecordAdapter adapter;
//    DateFormat df=new SimpleDateFormat("MM-dd HH:mm:ss");
    DateFormat df=new SimpleDateFormat("HH:mm:ss");
    long startAdd=0;
    long rttSum=0;
    long getSum=0;
    long rttT=0;
    long getT=0;
    long sendT=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_trans);

        fileCid=getIntent().getStringExtra("fileCid");
        Log.d(TAG, "onCreate: get File: "+fileCid);

        pref=getSharedPreferences("txtl",Context.MODE_PRIVATE);
        loginAccount=pref.getString("loginAccount",""); //当前登录的account，就是address

        //从数据库中查出每个的接收时间
        records=new LinkedList<>();
        records= DBHelper.getInstance(getApplicationContext(),loginAccount).listRecords(fileCid);
        Log.d(TAG, "onCreate: 查出通知数量："+records.size());
        recordsLv=findViewById(R.id.recordd_lv);
        adapter=new RecordAdapter(FileTransActivity.this,R.layout.item_records,records);
        recordsLv.setAdapter(adapter);

        if(!EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().register(this);
        }

//        // 统计时间
        rtt=findViewById(R.id.rtt);
        getAvr=findViewById(R.id.get_average);
        sendDuration=findViewById(R.id.send_duration);
        startAdd=records.get(0).t1; //发送端开始发送的时间
        sendT=records.get(0).t2-records.get(0).t1;
        sendDuration.setText(sendT+" ms");
        processData();

        //savelog
        saveLog=findViewById(R.id.save_log);
        saveLog.setOnClickListener(view -> {
            String head="平均rtt:"+rttT+
                    "\n平均接收时间:"+getT+
                    "\n发送时间:"+sendT+"\n";
            String dir= FileUtil.getAppExternalPath(this,"txtllog");
            String logDate=df.format(System.currentTimeMillis());
            try {
                File logFile=new File(dir+"/"+fileCid+"_"+logDate+".log");
                if(!logFile.exists()){
                    logFile.createNewFile();
                }
                Log.d(TAG, "onCreate: logpath: "+logFile.getAbsolutePath());
                FileWriter writer=new FileWriter(logFile);
                writer.write(head); writer.flush();

                for(TRecord tRecord:records){
                    String writeStr;
                    String user=tRecord.recordFrom;
                    String get1Str=df.format(tRecord.t1);
                    String get2Str=df.format(tRecord.t2);
                    long gap=tRecord.t2-tRecord.t1;
                    if(tRecord.type==0){
                        writeStr="自身节点,开始发送:"+get1Str+", 发送完毕:"+get2Str+", 耗时:"+gap+"\n";
                    }else{
                        writeStr="接收节点:"+user+", 开始接收:"+get1Str+", 接收完毕:"+get2Str+", 耗时:"+gap+"\n";
                    }
                    writer.write(writeStr); writer.flush();
                }
                writer.close();
                Toast.makeText(this, "保存log："+logFile.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void processData(){
        getSum=0;
        rttSum=0;
        for (int i=1;i<records.size();i++){
            getSum+=(records.get(i).t2-records.get(i).t1); //接收端自己从get到done的时间
            rttSum+=(records.get(i).t3-startAdd); //发送端从发送到接收的自己的时间，rtt
        }
        int recvNum=records.size()-1;
        if(recvNum==0){
            rtt.setText("未收到返回信息");
            getAvr.setText("未收到返回信息");
        }else{
            rttT=rttSum / recvNum;
            rtt.setText(rttT +" ms");
            getT=getSum / recvNum;
            getAvr.setText( getT+" ms");
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void getNewMsg(TRecord tRecord){
        Log.d(TAG, "getNewMsg: 拿到通知：");
        if(tRecord.cid.equals(fileCid)){
            records.add(tRecord);
            adapter.notifyDataSetChanged();
            processData();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().unregister(this);
        }
    }

    class RecordAdapter extends ArrayAdapter{
        Context context;
        LinkedList<TRecord> arecords;
        int resource;

        public RecordAdapter(@NonNull Context context, int resource, LinkedList<TRecord> records) {
            super(context, resource, records);
            this.context=context;
            this.resource=resource;
            this.arecords = records;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View v;
            RecordView recordView;
            TRecord tRecord=arecords.get(position);
            if(convertView == null){
                v= LayoutInflater.from(context).inflate(resource,parent,false);
                recordView=new RecordView(v);
                v.setTag(recordView);
            }else{
                v=convertView;
                recordView=(RecordView) v.getTag();
            }

            String user=tRecord.recordFrom;
            String get1Str=df.format(tRecord.t1);
            String get2Str=df.format(tRecord.t2);
            long gap=tRecord.t2-tRecord.t1;
            long rttt=tRecord.t3-startAdd;
            if(tRecord.type==0){
                recordView.user.setText("自身节点");
                recordView.duration.setText("开始:"+get1Str+", 发完:"+get2Str+", 耗时:"+gap);
            }else{
                recordView.user.setText("接收节点:"+user.substring(0,13)+"...");
                recordView.duration.setText("开始:"+get1Str+", 收完:"+get2Str+", 耗时:"+gap+", rtt:"+rttt);
            }

            return v;
        }

        class RecordView {
            TextView user;
            TextView duration;
            public RecordView(View v){
                user=v.findViewById(R.id.time_user);
                duration=v.findViewById(R.id.time_duration);
            }
        }
    }
}
