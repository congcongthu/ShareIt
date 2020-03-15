package com.sjtuopennetwork.shareit.album.gmx;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.sjtuopennetwork.shareit.R;

public class FileShow extends AppCompatActivity {
    //UI控件
    //PhotoView photoView;
    TextView file_name;
    TextView file_time;
    TextView file_path;
    //内存数据
    String filename;
    String filetime;
    String filepath;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_show);
        Intent it=getIntent();
        filename=it.getStringExtra("filename");
        filetime=it.getStringExtra("filetime");
        filepath=it.getStringExtra("filepath");

        file_name=findViewById(R.id.file_name_1);
        file_time=findViewById(R.id.file_time_1);
        file_path=findViewById(R.id.file_path_1);

        if(file_name.equals("")){
            file_name.setText("null");
        }else{
            file_name.setText(filename);
        }
        if(file_time.equals("")){
            file_time.setText("null");
        }else{
            file_time.setText(filetime);
        }
        if(file_path.equals("")){
            file_path.setText("null");
        }else{
            file_path.setText(filepath);
        }
    }
}
