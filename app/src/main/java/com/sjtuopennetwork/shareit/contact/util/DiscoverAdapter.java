package com.sjtuopennetwork.shareit.contact.util;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import com.shehuan.niv.NiceImageView;
import com.sjtuopennetwork.shareit.R;

import java.util.List;

public class DiscoverAdapter extends BaseAdapter {
    List<ResultContact> datas;


    @Override
    public int getCount() {
        return 0;
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        return null;
    }


    class ViewHolder{
        public NiceImageView avatar;
        public TextView name;
        public TextView addr;
        public Button addFriend;
        public CheckBox createGp;

        public ViewHolder(View v){
            avatar=v.findViewById(R.id.discover_avatar);
            name=v.findViewById(R.id.discover_name);
            addr=v.findViewById(R.id.discover_address);
            addFriend=v.findViewById(R.id.bt_add_discover_friend);
            createGp=v.findViewById(R.id.discover_create_group);
        }
    }
}
