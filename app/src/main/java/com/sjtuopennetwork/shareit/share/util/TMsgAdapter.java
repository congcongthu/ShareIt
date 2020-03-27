package com.sjtuopennetwork.shareit.share.util;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.sjtuopennetwork.shareit.R;
import com.sjtuopennetwork.shareit.share.ImageInfoActivity;
import com.sjtuopennetwork.shareit.share.StreamTestActivity;
import com.sjtuopennetwork.shareit.util.RoundImageView;
import com.sjtuopennetwork.shareit.util.ShareUtil;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

import sjtu.opennet.hon.Handlers;
import sjtu.opennet.hon.Textile;

public class TMsgAdapter extends BaseAdapter {

    private static final String TAG = "====TMsgAdapter";

    private List<TMsg> msgList;
    DateFormat df=new SimpleDateFormat("MM-dd HH:mm:ss");
    Context context;
    String threadid;

    public TMsgAdapter(Context context, List<TMsg> msgList, String threadid) {
        this.msgList = msgList;
        this.context = context;
        this.threadid=threadid;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        switch (getItemViewType(i)){
            case 0: //是文本
                return handleTextView(i,view,viewGroup);
            case 1: //是照片
                return handlePhotoView(i,view,viewGroup,false);
            case 2:
                return handlePhotoView(i,view,viewGroup,true);
            case 3:
                Log.d(TAG, "getView: getFile");
                return handleFileView(i,view,viewGroup);
            default:
                return null;
        }
    }

    private View handleTextView(int i, View view, ViewGroup viewGroup) {
        if(view==null){
            view=LayoutInflater.from(context).inflate(R.layout.item_msg_text,viewGroup,false);
            view.setTag(new TextVH(view));
        }
        if(view.getTag() instanceof TextVH){
            TextVH h=(TextVH) view.getTag();
            String username="";
            String useravatar="";
            if(msgList.get(i).ismine){
                username= ShareUtil.getMyName();
                useravatar=ShareUtil.getMyAvatar();
                h.send_text_right.setVisibility(View.VISIBLE); //右边的显示
                h.send_text_left.setVisibility(View.GONE); //左边的隐藏
                h.msg_name_r.setText(username);
                h.msg_time_r.setText(df.format(msgList.get(i).sendtime*1000));
                h.chat_words_r.setText(msgList.get(i).body);
                ShareUtil.setImageView(context,h.msg_avatar_r,useravatar,true);
            }else{
                String addr=msgList.get(i).author;
                username=ShareUtil.getOtherName(addr);
                useravatar=ShareUtil.getOtherAvatar(addr);
                h.send_text_left.setVisibility(View.VISIBLE); //左边的显示
                h.send_text_right.setVisibility(View.GONE); //右边的隐藏
                h.chat_words.setText(msgList.get(i).body);
                h.msg_name.setText(username);
                h.msg_time.setText(df.format(msgList.get(i).sendtime*1000));
                ShareUtil.setImageView(context,h.msg_avatar,useravatar,true);
            }
        }
        return view;
    }

    private View handlePhotoView(int i, View view, ViewGroup viewGroup, boolean isVideo) {
        Log.d(TAG, "handlePhotoView: pic_hash: "+ msgList.get(i).body);
        if(view==null){
            view=LayoutInflater.from(context).inflate(R.layout.item_msg_img,viewGroup,false);
            view.setTag(new PhotoVH(view));
        }
        if(view.getTag() instanceof PhotoVH){
            PhotoVH h=(PhotoVH) view.getTag();
            String username="";
            String useravatar="";
            if(msgList.get(i).ismine){
                username= ShareUtil.getMyName();
                useravatar=ShareUtil.getMyAvatar();
                h.send_photo_right.setVisibility(View.VISIBLE); //右边的显示
                h.send_photo_left.setVisibility(View.GONE); //左边的隐藏
                h.photo_name_r.setText(username);
                h.photo_time_r.setText(df.format(msgList.get(i).sendtime*1000));
                ShareUtil.setImageView(context,h.photo_avatar_r,useravatar,true);
                if(!isVideo){ //不是视频就是图片的hash
                    String[] hashName=msgList.get(i).body.split("##");
                    h.video_icon_r.setVisibility(View.GONE);
                    ShareUtil.setImageView(context,h.chat_photo_r,hashName[0],false);
                    h.chat_photo_r.setOnClickListener(v ->{
                        Intent it1=new Intent(context, ImageInfoActivity.class);
                        it1.putExtra("imghash", hashName[0]);
                        it1.putExtra("imgname",hashName[1]);
                        context.startActivity(it1);
                    });
                }else{ //是自己的视频，直接进入播放
                    String[] posterAndId_r=msgList.get(i).body.split("##"); //0是poster，1是Id，2是视频路径
                    Glide.with(context).load(posterAndId_r[0]).thumbnail(0.3f).into(h.chat_photo_r);
                    h.chat_photo_r.setOnClickListener(view1->{
                        Intent it12=new Intent(context, StreamTestActivity.class);
                        it12.putExtra("ismine",true);
                        it12.putExtra("videopath",posterAndId_r[1]);
                        context.startActivity(it12);
                    });
                }
            }else{
                String addr=msgList.get(i).author;
                username=ShareUtil.getOtherName(addr);
                useravatar=ShareUtil.getOtherAvatar(addr);
                h.send_photo_left.setVisibility(View.VISIBLE); //左边的显示
                h.send_photo_right.setVisibility(View.GONE); //右边的隐藏
                h.photo_name.setText(username);
                h.photo_time.setText(df.format(msgList.get(i).sendtime*1000));
                ShareUtil.setImageView(context,h.photo_avatar,useravatar,true);
                if(!isVideo){ //别人的图片
                    String[] hashName=msgList.get(i).body.split("##");
                    h.video_icon.setVisibility(View.GONE);
                    ShareUtil.setImageView(context,h.chat_photo,hashName[0],false);
                    h.chat_photo.setOnClickListener(v ->{
                        Intent it1=new Intent(context, ImageInfoActivity.class);
                        it1.putExtra("imghash", hashName[0]);
                        it1.putExtra("imgname",hashName[1]);
                        context.startActivity(it1);
                    });
                }else{ //是别人的视频
                    Glide.with(context).load(R.drawable.ic_album).thumbnail(0.3f).into(h.chat_photo);
                    h.chat_photo.setOnClickListener(view1->{
                        Intent it11=new Intent(context, StreamTestActivity.class);
                        it11.putExtra("streamid",msgList.get(i).body);
                        it11.putExtra("ismine",false);
                        context.startActivity(it11);
                    });
                }
            }
        }
        return view;
    }

    private View handleFileView(int i, View view, ViewGroup viewGroup){
        if(view==null){
            view=LayoutInflater.from(context).inflate(R.layout.item_msg_file,viewGroup,false);
            view.setTag(new FileVH(view));
        }

        if(view.getTag() instanceof FileVH){
            FileVH h=(FileVH)view.getTag();
            String username="";
            String useravatar="";
            String[] hashName=msgList.get(i).body.split("##");
            Log.d(TAG, "handleFileView: "+hashName[0]+" "+hashName[1]);
            if(msgList.get(i).ismine){
                username= ShareUtil.getMyName();
                useravatar=ShareUtil.getMyAvatar();
                h.send_file_right.setVisibility(View.VISIBLE); //右边的显示
                h.send_file_left.setVisibility(View.GONE); //左边的隐藏
                h.file_name_r.setText(username);
                h.file_time_r.setText(df.format(msgList.get(i).sendtime*1000));
                h.file_name_r.setText(hashName[1]);
                ShareUtil.setImageView(context,h.file_avatar_r,useravatar,true);
            }else{
                String addr=msgList.get(i).author;
                username=ShareUtil.getOtherName(addr);
                useravatar=ShareUtil.getOtherAvatar(addr);
                h.send_file_left.setVisibility(View.VISIBLE); //左边的显示
                h.send_file_right.setVisibility(View.GONE); //右边的隐藏
                h.file_name.setText(username);
                h.file_time.setText(df.format(msgList.get(i).sendtime*1000));
                h.file_name.setText(hashName[1]);
                ShareUtil.setImageView(context,h.file_avatar,useravatar,true);
                h.send_file_left.setOnClickListener(v -> {
                    AlertDialog.Builder downFile=new AlertDialog.Builder(context);
                    downFile.setTitle("下载文件");
                    downFile.setMessage("确定下载 "+ hashName[1] +" 吗？");
                    downFile.setPositiveButton("下载", (dialog, which) -> {
                        Textile.instance().files.content(hashName[0], new Handlers.DataHandler() {
                            @Override
                            public void onComplete(byte[] data, String media) {
                                String res= ShareUtil.storeSyncFile(data,hashName[1]);
                                System.out.println("======下载成功"+res);
                            }

                            @Override
                            public void onError(Exception e) {

                            }
                        });
                    });
                    downFile.setNegativeButton("取消", (dialog, which) -> Toast.makeText(context,"已取消",Toast.LENGTH_SHORT).show());
                    downFile.show();
                });
            }
        }
        return view;
    }


    @Override
    public int getItemViewType(int position) {
        return msgList.get(position).msgtype;
    }
    @Override
    public int getCount() {
        return msgList.size();
    }
    @Override
    public int getViewTypeCount() {
        return 4;
    }
    @Override
    public Object getItem(int position) {
        return msgList.get(position);
    }
    @Override
    public long getItemId(int position) {
        return position;
    }
    @Override
    public boolean isEnabled(int position) {
        return false;
    }
    public static class TextVH{
        public TextView msg_name,msg_time,chat_words;
        public TextView msg_name_r,msg_time_r,chat_words_r;
        public RoundImageView msg_avatar,msg_avatar_r;
        public LinearLayout send_text_left,send_text_right;

        public TextVH(View v){
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
    public static class PhotoVH{
        public TextView photo_name,photo_time;
        public TextView photo_name_r,photo_time_r;
        public RoundImageView photo_avatar,photo_avatar_r;
        public ImageView chat_photo,chat_photo_r,video_icon,video_icon_r;
        public LinearLayout send_photo_left,send_photo_right;

        public PhotoVH(View v){
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
    public static class FileVH{
        public TextView file_user,file_time,file_name;
        public TextView file_user_r,file_time_r,file_name_r;
        public RoundImageView file_avatar,file_avatar_r;
        public LinearLayout send_file_left,send_file_right;

        public FileVH(View v){
            file_user=v.findViewById(R.id.send_file_user);
            file_time=v.findViewById(R.id.send_file_time);
            file_name=v.findViewById(R.id.send_file_name);
            file_avatar=v.findViewById(R.id.send_file_avatar);
            file_user_r=v.findViewById(R.id.send_file_user_r);
            file_time_r=v.findViewById(R.id.send_file_time_r);
            file_name_r=v.findViewById(R.id.send_file_name_r);
            file_avatar_r=v.findViewById(R.id.send_file_avatar_r);
            send_file_left=v.findViewById(R.id.send_file_left);
            send_file_right=v.findViewById(R.id.send_file_right);
        }
    }
}
