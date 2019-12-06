package com.sjtuopennetwork.shareit.contact.util;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;


import com.sjtuopennetwork.shareit.R;
import com.sjtuopennetwork.shareit.util.FileUtil;
import com.sjtuopennetwork.shareit.util.RoundImageView;

import java.util.List;

import sjtu.opennet.hon.Handlers;
import sjtu.opennet.hon.Textile;

public class ResultAdapter extends ArrayAdapter {
    private static final String TAG = "=============================";

    Context context;
    int resource;
    List<ResultContact> resultContacts;
    byte[] img;

    public ResultAdapter(Context context, int resource, List objects) {
        super(context, resource, objects);
        this.context=context;
        this.resource=resource;
        this.resultContacts=objects;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v;
        ViewHolder vh;

        if(convertView==null){
            v= LayoutInflater.from(context).inflate(resource,parent,false);
            vh=new ViewHolder(v);
            v.setTag(vh);
        }else{
            v=convertView;
            vh=(ViewHolder)v.getTag();
        }

        if(resultContacts.get(position).avatarhash.equals("")){ //如果没有设置头像
            vh.avatar.setImageResource(R.drawable.ic_default_avatar);
        }else{ //设置过头像
            img=resultContacts.get(position).avatar;
            if(img==null){
                setAvatar(vh.avatar,resultContacts.get(position).avatarhash);
            }else{
                vh.avatar.setImageBitmap(BitmapFactory.decodeByteArray(img,0,img.length));
            }
        }
        vh.name.setText(resultContacts.get(position).name);
        vh.addr.setText(resultContacts.get(position).address);
        return v;
    }

    class ViewHolder{
        public RoundImageView avatar;
        public TextView name;
        public TextView addr;

        public ViewHolder(View v){
            avatar=v.findViewById(R.id.contact_result_avatar);
            name=v.findViewById(R.id.result_name);
            addr=v.findViewById(R.id.result_address);
        }
    }


    private void setAvatar(ImageView imageView, String avatarHash) {

        Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        String newPath = msg.getData().getString("newPath");
                        imageView.setImageBitmap(BitmapFactory.decodeFile(newPath));
                }
            }
        };

        String avatarPath = FileUtil.getFilePath(avatarHash);
        if (avatarPath.equals("null")) { //如果没有存储过这个头像文件
            Textile.instance().ipfs.dataAtPath("/ipfs/" + avatarHash + "/0/small/content", new Handlers.DataHandler() {
                @Override
                public void onComplete(byte[] data, String media) {
                    String newPath = FileUtil.storeFile(data, avatarHash);
                    Message msg = new Message();
                    msg.what = 1;
                    Bundle b = new Bundle();
                    b.putString("newPath", newPath);
                    msg.setData(b);
                    handler.sendMessage(msg);
                }

                @Override
                public void onError(Exception e) {

                }
            });
        } else { //如果已经存储过这个头像
            imageView.setImageBitmap(BitmapFactory.decodeFile(avatarPath));
        }
    }
}
