package com.sjtuopennetwork.shareit.contact;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.shehuan.niv.NiceImageView;
import com.sjtuopennetwork.shareit.R;

import java.util.List;

public class SearchResultAdapter extends ArrayAdapter {

    Context context;
    int resource;
    List<SearchResultContact> resultContacts;

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

        if(resultContacts.get(position).avatar==null){
            if(resultContacts.get(position).avatarPath.equals("null")){ //没有头像的新用户
                vh.avatar.setImageResource(R.drawable.ic_default_avatar);
            }else{ //已添加联系人
                vh.avatar.setImageBitmap(BitmapFactory.decodeFile(resultContacts.get(position).avatarPath));
            }
        }else{
            byte[] data=resultContacts.get(position).avatar;
            vh.avatar.setImageBitmap(BitmapFactory.decodeByteArray(data,0,data.length));
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
