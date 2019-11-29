package com.sjtuopennetwork.shareit.share.util;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;


import com.bumptech.glide.Glide;
import com.sjtuopennetwork.shareit.R;
import com.sjtuopennetwork.shareit.util.FileUtil;
import com.sjtuopennetwork.shareit.util.RoundImageView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

import sjtu.opennet.hon.Handlers;
import sjtu.opennet.hon.Textile;

public class DialogAdapter extends ArrayAdapter {
    private static final String TAG = "==================";

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
        TDialog tDialog = datas.get(position);
        if (convertView == null) {
            v = LayoutInflater.from(context).inflate(resource, parent, false);
            vh = new ViewHolder(v);
            v.setTag(vh);
        } else {
            v = convertView;
            vh = (ViewHolder) v.getTag();
        }

        if (tDialog.imgpath.equals("tongzhi")) {
            vh.headImg.setImageResource(R.drawable.ic_notification_img);
        } else {
            if (tDialog.isSingle) { //如果是单人的，就设置头像
                setAvatar(vh.headImg, tDialog.imgpath);
            } else { //如果是多人的就设置图片
                setPhoto(vh.headImg, tDialog.imgpath);
            }
        }

        if (tDialog.isRead) {
            vh.isRead.setVisibility(View.GONE);
        } else {
            vh.isRead.setVisibility(View.VISIBLE);
        }
        DateFormat df = new SimpleDateFormat("MM-dd HH:mm");

        vh.lastMsg.setText(tDialog.lastmsg);
        vh.lastMsgDate.setText(df.format(tDialog.lastmsgdate * 1000));
        vh.threadName.setText(tDialog.threadname);

        return v;
    }

    class ViewHolder {
        private TextView threadName, lastMsg, isRead, lastMsgDate;
        private ImageView headImg;

        public ViewHolder(View v) {
            threadName = v.findViewById(R.id.item_thread_name);
            lastMsg = v.findViewById(R.id.item_thread_lastmsg);
            lastMsgDate = v.findViewById(R.id.item_thread_lastmsgdate);
            headImg = v.findViewById(R.id.item_thread_img);
            isRead = v.findViewById(R.id.item_thread_badge);
        }
    }

    private void setAvatar(ImageView imageView, String avatarHash) {

        Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        String newPath = msg.getData().getString("newPath");
                        Log.d(TAG, "handleMessage: 拿到头像：" + newPath);
                        Glide.with(context).load(newPath).thumbnail(0.3f).into(imageView);
                }
            }
        };

        String avatarPath = FileUtil.getFilePath(avatarHash);
        if (avatarPath.equals("null")) { //如果没有存储过这个头像文件
            System.out.println(avatarPath);
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

    private void setPhoto(ImageView imageView, String fileHash) {

        Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        String newPath = msg.getData().getString("newPath");
                        Glide.with(context).load(newPath).thumbnail(0.3f).into(imageView);
                }
            }
        };

        String filePath = FileUtil.getFilePath(fileHash);
        if (filePath.equals("null")) { //如果没有存储过图片
            Textile.instance().files.content(fileHash, new Handlers.DataHandler() {
                @Override
                public void onComplete(byte[] data, String media) {
                    String newPath = FileUtil.storeFile(data, fileHash);
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
        } else {
            imageView.setImageBitmap(BitmapFactory.decodeFile(filePath));
        }
    }
}
