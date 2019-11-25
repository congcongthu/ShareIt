package com.sjtuopennetwork.shareit.contact.util;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.shehuan.niv.NiceImageView;
import com.sjtuopennetwork.shareit.R;

import java.util.List;

import sjtu.opennet.hon.Handlers;
import sjtu.opennet.hon.Textile;

public class ResultAdapter extends ArrayAdapter {

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
                String getAvatar="/ipfs/" + resultContacts.get(position).avatarhash + "/0/small/content";
                Textile.instance().ipfs.dataAtPath(getAvatar, new Handlers.DataHandler() {
                    @Override
                    public void onComplete(byte[] data, String media) {
                        img=data;
                        vh.avatar.setImageBitmap(BitmapFactory.decodeByteArray(img,0,img.length));
                    }
                    @Override
                    public void onError(Exception e) {
                    }
                });
            }else{
                vh.avatar.setImageBitmap(BitmapFactory.decodeByteArray(img,0,img.length));
            }
        }
        vh.name.setText(resultContacts.get(position).name);
        vh.addr.setText(resultContacts.get(position).address);
        return v;
    }

    class ViewHolder{
        public NiceImageView avatar;
        public TextView name;
        public TextView addr;

        public ViewHolder(View v){
            avatar=v.findViewById(R.id.result_avatar);
            name=v.findViewById(R.id.result_name);
            addr=v.findViewById(R.id.result_address);
        }
    }
}
