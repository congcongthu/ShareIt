package com.sjtuopennetwork.shareit.share;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
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
import com.sjtuopennetwork.shareit.util.ShareUtil;

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

import sjtu.opennet.hon.Handlers;
import sjtu.opennet.hon.Stream;
import sjtu.opennet.hon.Textile;
import sjtu.opennet.stream.util.FileUtil;

public class FileTransActivity extends AppCompatActivity {

    private static final String TAG = "===================FileTransActivity";

    private TextView trans_size;
    private TextView trans_rtt;
    private TextView trans_rec;
    private TextView trans_send;
    private ListView recordsLv;
    private Button saveLog;

    private String fileCid;
    private String fileSizeCid;
    private LinkedList<TRecord> records;
    SharedPreferences pref;
    String loginAccount;
    RecordAdapter adapter;
//    DateFormat df=new SimpleDateFormat("MM-dd HH:mm:ss");
    DateFormat df=new SimpleDateFormat("HH:mm:ss:SSS");
    long startAdd=0;
    long rttSum=0;
    long getSum=0;
    long rttT=0;
    long getT=0;
    long sendT=0;
    int filesize=0;
    boolean isStream=false;

    Handler handler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            int fileSizeInt=(int)msg.obj;
            Log.d(TAG, "handleMessage: handle文件大小："+fileSizeInt);
            trans_size.setText("文件大小:"+fileSizeInt+" B");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_trans);

        fileCid=getIntent().getStringExtra("fileCid");
        fileSizeCid=getIntent().getStringExtra("fileSizeCid");
        isStream=getIntent().getBooleanExtra("isStream",false);
        Log.d(TAG, "onCreate: get File: "+fileCid);

        pref=getSharedPreferences("txtl",Context.MODE_PRIVATE);
        loginAccount=pref.getString("loginAccount",""); //当前登录的account，就是address

        //从数据库中查出每个的接收时间
        records=new LinkedList<>();
        records= DBHelper.getInstance(getApplicationContext(),loginAccount).listRecords(fileCid);
        Log.d(TAG, "onCreate: records size: "+records.size());
        recordsLv=findViewById(R.id.recordd_lv);
        adapter=new RecordAdapter(FileTransActivity.this,R.layout.item_records,records);
        recordsLv.setAdapter(adapter);

        //显示文件大小
        trans_size=findViewById(R.id.file_trans_size);
        if(isStream){
            File file=new File(fileSizeCid);
            trans_size.setText("文件大小:"+file.length()+" B");
        }else {
            Textile.instance().ipfs.dataAtPath(fileSizeCid, new Handlers.DataHandler() {
                @Override
                public void onComplete(byte[] data, String media) {
                    Message msg = handler.obtainMessage();
                    msg.what = 9;
                    msg.obj = data.length;
                    filesize = data.length;
                    Log.d(TAG, "onComplete: 文件大小：" + data.length);
                    handler.sendMessage(msg);
                }

                @Override
                public void onError(Exception e) {

                }
            });
        }
//        Textile.instance().files.content(fileSizeCid, new Handlers.DataHandler() {
//            @Override
//            public void onComplete(byte[] data, String media) {
//                Message msg=handler.obtainMessage();
//                msg.what=9;
//                msg.obj=data.length;
//                filesize=data.length;
//                Log.d(TAG, "onComplete: 文件大小："+data.length);
//                handler.sendMessage(msg);
//            }
//            @Override
//            public void onError(Exception e) {
//            }
//        });

        // 统计时间
        trans_rtt=findViewById(R.id.file_trans_rtt);
        trans_rec=findViewById(R.id.file_trans_rec_t);
        trans_send=findViewById(R.id.file_trans_send_t);
        startAdd=records.get(0).t1; //发送端开始发送的时间
        sendT=records.get(0).t2-records.get(0).t1;
        if(isStream){
            trans_send.setText("发送时间:未统计");
        }else{
            trans_send.setText("发送时间:"+sendT+" ms");
        }
        if(!EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().register(this);
        }
        Log.d(TAG, "onCreate: records size 2:"+records.size());
        processData();

        //savelog
        saveLog=findViewById(R.id.save_log);
        saveLog.setOnClickListener(view -> {
            DateFormat dfd=new SimpleDateFormat("MM-dd HH:mm");
            String head="文件大小:"+filesize+
                    "\n平均rtt:"+rttT+
                    "\n平均接收时间:"+getT+
                    "\n发送时间:"+sendT+"\n";
            String dir= FileUtil.getAppExternalPath(this,"txtllog");
            String logDate=dfd.format(System.currentTimeMillis());
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
                    long grtt=tRecord.t3-startAdd;
                    if(tRecord.type==0){
                        writeStr="自身节点,开始:"+get1Str+", 发完:"+get2Str+", 耗时:"+gap+"ms\n";
                    }else{
                        writeStr="接收节点:"+user+", 开始:"+get1Str+", 收完:"+get2Str+", 耗时:"+gap+"ms, rtt:"+grtt+"ms\n";
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
        if(isStream){
            rttSum=0;
            for (int i=1;i<records.size();i++){
                rttSum+=(records.get(i).t3-startAdd); //发送端从发送到接收的自己的时间，rtt
            }
            int recvNum=records.size()-1;
            Log.d(TAG, "processData: recvNum: "+recvNum);
            if(recvNum==0){
                trans_rtt.setText("平均RTT:未收到返回");
                trans_rec.setText("平均接收时间:未统计");
            }else{
                rttT=rttSum / recvNum;
                trans_rtt.setText("平均RTT:"+rttT +" ms");
                trans_rec.setText( "平均接收时间: 未统计");
            }
        }else{
            getSum=0;
            rttSum=0;
            for (int i=1;i<records.size();i++){
                getSum+=(records.get(i).t2-records.get(i).t1); //接收端自己从get到done的时间
                rttSum+=(records.get(i).t3-startAdd); //发送端从发送到接收的自己的时间，rtt
            }
            int recvNum=records.size()-1;
            Log.d(TAG, "processData: recvNum: "+recvNum);
            if(recvNum==0){
                trans_rtt.setText("平均RTT:未收到返回");
                trans_rec.setText("平均接收时间:未收到返回");
            }else{
                rttT=rttSum / recvNum;
                trans_rtt.setText("平均RTT:"+rttT +" ms");
                getT=getSum / recvNum;
                trans_rec.setText( "平均接收时间:"+getT+" ms");
            }
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
            if(isStream){
                get1Str="0";
                get2Str="0";
            }
            long gap=tRecord.t2-tRecord.t1;
            long rttt=tRecord.t3-startAdd;
            if(tRecord.type==0){
                Log.d(TAG, "getView: 显示自己："+position);
                recordView.user.setText("自身节点");
                if(isStream){
                    recordView.duration.setText("0");
                }else{
                    recordView.duration.setText("开始:"+get1Str+",  发完:"+get2Str+"\n耗时:"+gap+"ms");
                }
            }else{
                Log.d(TAG, "getView: 显示接收："+position);
                recordView.user.setText("接收节点:"+user.substring(0,13)+"...");
                recordView.duration.setText("开始:"+get1Str+",  收完:"+get2Str+"\n耗时:"+gap+"ms,  rtt:"+rttt+"ms");
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
