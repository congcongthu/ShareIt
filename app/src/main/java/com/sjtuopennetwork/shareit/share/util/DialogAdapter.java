package com.sjtuopennetwork.shareit.share.util;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.sjtuopennetwork.shareit.R;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

public class DialogAdapter extends ArrayAdapter {
    private List<TDialog> datas;
    private Context context;
    private int resource;

    public DialogAdapter(Context context, int resource, List<TDialog> datas) {
        super(context, resource, datas);
        this.datas = datas;
        this.context = context;
        this.resource = resource;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View v;
        ViewHolder vh;
        TDialog tDialog=datas.get(position);
        if(convertView==null){
            v= LayoutInflater.from(context).inflate(resource,parent,false);
            vh=new ViewHolder(v);
            v.setTag(vh);
        }else{
            v=convertView;
            vh=(ViewHolder)v.getTag();
        }

        if(tDialog.imgpath.equals("null")){
            vh.headImg.setImageResource(R.drawable.back_gray);
        }else if(tDialog.imgpath.equals("tongzhi")){
            vh.headImg.setImageResource(R.drawable.ic_notification_img);
        } else{
            vh.headImg.setImageBitmap(BitmapFactory.decodeFile(tDialog.imgpath));
        }

        if(tDialog.isRead){
            vh.isRead.setVisibility(View.GONE);
        }
        DateFormat df=new SimpleDateFormat("MM-dd HH:mm");

        vh.lastMsg.setText(tDialog.lastmsg);
        vh.lastMsgDate.setText(df.format(tDialog.lastmsgdate*1000));
        vh.threadName.setText(tDialog.threadname);

        return v;
    }

    class ViewHolder{
        private TextView threadName,lastMsg,isRead,lastMsgDate;
        private ImageView headImg;

        public ViewHolder(View v){
            threadName=v.findViewById(R.id.item_thread_name);
            lastMsg=v.findViewById(R.id.item_thread_lastmsg);
            lastMsgDate=v.findViewById(R.id.item_thread_lastmsgdate);
            headImg=v.findViewById(R.id.item_thread_img);
            isRead=v.findViewById(R.id.item_thread_badge);
        }
    }
}
