package com.sjtuopennetwork.shareit.contact;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.shehuan.niv.NiceImageView;
import com.sjtuopennetwork.shareit.R;

import java.util.List;

import io.textile.textile.Handlers;
import io.textile.textile.Textile;

public class SearchResultAdapter extends ArrayAdapter {

    Context context;
    int resource;
    List<SearchResultContact> resultContacts;
    byte[] img;

    public SearchResultAdapter(Context context, int resource, List objects) {
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
            System.out.println("=====没有设置头像："+resultContacts.get(position).getName());
            vh.avatar.setImageResource(R.drawable.ic_default_avatar);
        }else{ //设置过头像
            System.out.println("=====设置了头像："+resultContacts.get(position).getName()+" "+resultContacts.get(position).getAvatarhash());
            img=resultContacts.get(position).getAvatar();
            if(img==null){
                Textile.instance().ipfs.dataAtPath("/ipfs/" + resultContacts.get(position).getAvatarhash() + "/0/small/content", new Handlers.DataHandler() {
                    @Override
                    public void onComplete(byte[] data, String media) {
                        System.out.println("===============获得了头像");
                        img=data;
                        vh.avatar.setImageBitmap(BitmapFactory.decodeByteArray(img,0,img.length));
                    }
                    @Override
                    public void onError(Exception e) {
                        System.out.println("=========获得头像失败");
                        vh.avatar.setImageResource(R.drawable.ic_default_avatar);
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
