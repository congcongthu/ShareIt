package com.sjtuopennetwork.shareit.share.util;

import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.sjtuopennetwork.shareit.R;
import com.sjtuopennetwork.shareit.share.ImageInfoActivity;
import com.sjtuopennetwork.shareit.share.VideoPlayActivity;
import com.sjtuopennetwork.shareit.util.FileUtil;
import com.sjtuopennetwork.shareit.util.RoundImageView;

import org.greenrobot.eventbus.EventBus;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

import sjtu.opennet.hon.Handlers;
import sjtu.opennet.hon.Textile;
import sjtu.opennet.textilepb.Model;

public class MsgAdapter extends BaseAdapter {

    private static final String TAG = "========================";

    private LayoutInflater layoutInflater;
    private List<TMsg> msgList;
    DateFormat df=new SimpleDateFormat("MM-dd HH:mm:ss");
    String avatarpath;
    Context context;

    public MsgAdapter(Context context, List<TMsg> msgList,String avatarpath) {
        this.context=context;
        this.layoutInflater = LayoutInflater.from(context);
        this.msgList = msgList;
        this.avatarpath=avatarpath;
    }

    public static class TextViewHolder{
        public TextView msg_name,msg_time,chat_words;
        public TextView msg_name_r,msg_time_r,chat_words_r;
        public RoundImageView msg_avatar,msg_avatar_r;
        public LinearLayout send_text_left,send_text_right;

        public TextViewHolder(View v){
            msg_name=v.findViewById(R.id.msg_name);
            msg_time=v.findViewById(R.id.msg_time);
            chat_words=v.findViewById(R.id.chat_words);
            msg_avatar=v.findViewById(R.id.msg_avatar);
            msg_name_r=v.findViewById(R.id.msg_name_r);
            msg_time_r=v.findViewById(R.id.msg_time_r);
            chat_words_r=v.findViewById(R.id.chat_words_r);
            msg_avatar_r=v.findViewById(R.id.msg_avatar_r);
            send_text_left=v.findViewById(R.id.send_msg_left);
            send_text_right=v.findViewById(R.id.send_msg_right);
        }
    }

    public static class PhotoViewHolder{
        public TextView photo_name,photo_time;
        public TextView photo_name_r,photo_time_r;
        public RoundImageView photo_avatar,photo_avatar_r;
        public ImageView chat_photo,chat_photo_r,video_icon,video_icon_r;
        public LinearLayout send_photo_left,send_photo_right;

        public PhotoViewHolder(View v){
            photo_name=v.findViewById(R.id.photo_name);
            photo_time=v.findViewById(R.id.photo_time);
            photo_avatar=v.findViewById(R.id.photo_avatar);
            chat_photo=v.findViewById(R.id.chat_photo);
            photo_name_r=v.findViewById(R.id.photo_name_r);
            photo_time_r=v.findViewById(R.id.photo_time_r);
            photo_avatar_r=v.findViewById(R.id.photo_avatar_r);
            chat_photo_r=v.findViewById(R.id.chat_photo_r);
            send_photo_left=v.findViewById(R.id.send_photo_left);
            send_photo_right=v.findViewById(R.id.send_photo_right);
            video_icon=v.findViewById(R.id.video_icon);
            video_icon_r=v.findViewById(R.id.video_icon_r);
        }
    }

    @Override
    public int getViewTypeCount() {
        return 3;
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).msgtype;
    }

    @Override
    public int getCount() {
        return msgList.size();
    }

    @Override
    public TMsg getItem(int position) {
        return msgList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        Log.d(TAG, "getView: getView被调用："+i);
        switch (getItemViewType(i)){
            case 0: //是文本
                return handleTextView(i,view,viewGroup);
            case 1: //是照片
                return handlePhotoView(i,view,viewGroup,false);
            case 2:
                return handlePhotoView(i,view,viewGroup,true);
            default:
                return null;
        }
    }

    private View handleTextView(int i, View view, ViewGroup viewGroup){
        if(view==null){
            view=layoutInflater.inflate(R.layout.item_msg_text,viewGroup,false);
            view.setTag(new TextViewHolder(view));
        }
        if(view.getTag() instanceof TextViewHolder){
            TextViewHolder h=(TextViewHolder) view.getTag();
            String avatarPath= FileUtil.getFilePath(msgList.get(i).authoravatar);
            if(msgList.get(i).ismine){ //如果是自己的消息
                h.send_text_right.setVisibility(View.VISIBLE); //右边的显示
                h.send_text_left.setVisibility(View.GONE); //左边的隐藏
                h.msg_name_r.setText(msgList.get(i).authorname);
                h.msg_time_r.setText(df.format(msgList.get(i).sendtime*1000));
                if(!msgList.get(i).authoravatar.equals("")){
                    setAvatar(h.msg_avatar_r,avatarPath,msgList.get(i).authoravatar);
                }
                h.chat_words_r.setText(msgList.get(i).body);
            }else{ //不是自己的消息
                h.send_text_left.setVisibility(View.VISIBLE); //左边的显示
                h.send_text_right.setVisibility(View.GONE); //右边的隐藏
                h.msg_name.setText(msgList.get(i).authorname);
                h.msg_time.setText(df.format(msgList.get(i).sendtime*1000));
                if(!msgList.get(i).authoravatar.equals("")) {
                    setAvatar(h.msg_avatar, avatarPath, msgList.get(i).authoravatar);
                }else{
                    Glide.with(context).load(R.drawable.ic_default_avatar).thumbnail(0.3f).into(h.msg_avatar);
                }
                h.chat_words.setText(msgList.get(i).body);
            }
        }
        return view;
    }

    private View handlePhotoView(int i, View view, ViewGroup viewGroup, boolean isVideo){
        if(view==null){
            view=layoutInflater.inflate(R.layout.item_msg_img,viewGroup,false);
            view.setTag(new PhotoViewHolder(view));
        }
        if(view.getTag() instanceof PhotoViewHolder){
            PhotoViewHolder h=(PhotoViewHolder) view.getTag();
            String avatarPath= FileUtil.getFilePath(msgList.get(i).authoravatar);
            String msgBody=msgList.get(i).body;
            if(msgList.get(i).ismine){ //如果是自己的图片消息
                h.send_photo_right.setVisibility(View.VISIBLE); //右边的显示
                h.send_photo_left.setVisibility(View.GONE); //左边的隐藏
                h.photo_name_r.setText(msgList.get(i).authorname);
                h.photo_time_r.setText(df.format(msgList.get(i).sendtime*1000));
                if(!msgList.get(i).authoravatar.equals("")) {
                    setAvatar(h.photo_avatar_r, avatarPath, msgList.get(i).authoravatar); //设置头像
                }
                if(!isVideo){ //不是视频就是图片，就隐藏播放图标
                    h.video_icon_r.setVisibility(View.GONE);
                    Glide.with(context).load(msgBody).thumbnail(0.3f).into(h.chat_photo_r);
                    h.chat_photo_r.setOnClickListener(v -> {
                        Intent it1=new Intent(context, ImageInfoActivity.class);
                        it1.putExtra("imgpath", msgBody);
                        context.startActivity(it1);
                    });
                }else{ //是视频就要解出缩略图的hash
                    String[] posterAndId_r=msgBody.split("##"); //0是poster，1是Id，2是视频路径
                    Log.d(TAG, "handlePhotoView: 自己视频的消息的body："+posterAndId_r[0]+" "+posterAndId_r[1]+" "+posterAndId_r[2]);
                    Glide.with(context).load(posterAndId_r[0]).thumbnail(0.3f).into(h.chat_photo_r);
                    h.chat_photo_r.setOnClickListener(view1 -> {
                        Intent it=new Intent(context, VideoPlayActivity.class);
                        it.putExtra("ismine",true);
                        it.putExtra("videoid",posterAndId_r[1]);
                        it.putExtra("videopath",posterAndId_r[2]);
                        context.startActivity(it);
                    });
                }
            }else{ //不是自己的消息
                h.send_photo_left.setVisibility(View.VISIBLE); //左边的显示
                h.send_photo_right.setVisibility(View.GONE); //右边的隐藏
                h.photo_name.setText(msgList.get(i).authorname);
                h.photo_time.setText(df.format(msgList.get(i).sendtime*1000));
                if(!msgList.get(i).authoravatar.equals("")) {
                    setAvatar(h.photo_avatar, avatarPath, msgList.get(i).authoravatar);
                }
                if(!isVideo){ //不是视频就隐藏播放图标
                    h.video_icon.setVisibility(View.GONE);
                    Glide.with(context).load(msgBody).thumbnail(0.3f).into(h.chat_photo);
                    h.chat_photo.setOnClickListener(v -> {
                        Intent it1=new Intent(context, ImageInfoActivity.class);
                        it1.putExtra("imgpath", msgBody);
                        context.startActivity(it1);
                    });
                }else{ //是视频就setVideo
                    String[] posterAndId=msgBody.split("##"); //0是poster，1是Id
                    Log.d(TAG, "handlePhotoView: 收到视频的消息的body："+posterAndId[0]+" "+posterAndId[1]+" "+posterAndId[2]);
                    Glide.with(context).load(posterAndId[0]).thumbnail(0.3f).into(h.chat_photo);
                    h.chat_photo.setOnClickListener(view1 -> {
                        Intent it=new Intent(context, VideoPlayActivity.class);
                        it.putExtra("ismine",false);
                        it.putExtra("videoid",posterAndId[1]);
                        context.startActivity(it);
                    });
                }
            }
        }
        return view;
    }

    private void setAvatar(RoundImageView imageView, String avatarPath, String avatarHash){



        Handler handler=new Handler(){
            @Override
            public void handleMessage(Message msg) {
                switch(msg.what){
                    case 1:
                        String newPath=msg.getData().getString("newPath");
                        Log.d(TAG, "handleMessage: 拿到头像："+newPath);
                        Glide.with(context).load(newPath).thumbnail(0.3f).into(imageView);
                }
            }
        };

        if(avatarPath.equals("null")){ //如果没有存储过这个头像文件
            Textile.instance().ipfs.dataAtPath("/ipfs/" + avatarHash + "/0/small/content", new Handlers.DataHandler() {
                @Override
                public void onComplete(byte[] data, String media) {
                    String newPath=FileUtil.storeFile(data,avatarHash);
                    Message msg=new Message(); msg.what=1;
                    Bundle b=new Bundle();b.putString("newPath",newPath);
                    msg.setData(b);
                    handler.sendMessage(msg);
                }
                @Override
                public void onError(Exception e) {
                    e.printStackTrace();
                }
            });
        }else{ //如果已经存储过这个头像
            Glide.with(context).load(avatarPath).thumbnail(0.3f).into(imageView);
        }
    }

    @Override
    public boolean isEnabled(int position) {
        return false;
    }

}
