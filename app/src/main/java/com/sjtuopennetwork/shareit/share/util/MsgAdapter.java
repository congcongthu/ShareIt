package com.sjtuopennetwork.shareit.share.util;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.shehuan.niv.NiceImageView;
import com.sjtuopennetwork.shareit.R;
import com.sjtuopennetwork.shareit.util.FileUtil;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

import io.textile.textile.Handlers;
import io.textile.textile.Textile;

public class MsgAdapter extends BaseAdapter {

    private LayoutInflater layoutInflater;
    private List<TMsg> msgList;
    DateFormat df=new SimpleDateFormat("MM-dd HH:mm:ss");

    public MsgAdapter(Context context, List<TMsg> msgList) {
        this.layoutInflater = LayoutInflater.from(context);
        this.msgList = msgList;
    }

    static class TextViewHolder{
        public TextView msg_name,msg_time,chat_words;
        public TextView msg_name_r,msg_time_r,chat_words_r;
        public NiceImageView msg_avatar,msg_avatar_r;
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

    static class PhotoViewHolder{
        public TextView photo_name,photo_time;
        public TextView photo_name_r,photo_time_r;
        public NiceImageView photo_avatar,photo_avatar_r;
        public ImageView chat_photo,chat_photo_r;
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
        }
    }

    @Override
    public int getViewTypeCount() {
        return 2;
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
        switch (getItemViewType(i)){
            case 0: //是文本
                return handleTextView(i,view,viewGroup);
            case 1: //是照片
                return handlePhotoView(i,view,viewGroup);
            default:
                return null;
        }
    }

    private View handleTextView(int i, View view, ViewGroup viewGroup){
        if(view==null){
            view=layoutInflater.inflate(R.layout.item_msg_text,viewGroup,false);
            view.setTag(new TextViewHolder(view));
        }
        if(view != null && view.getTag() instanceof TextViewHolder){
            TextViewHolder h=(TextViewHolder) view.getTag();
            String avatarPath= FileUtil.getFilePath(msgList.get(i).authoravatar);
            if(msgList.get(i).ismine){ //如果是自己的消息
                h.send_text_left.setVisibility(View.GONE); //左边的隐藏
                h.msg_name_r.setText(msgList.get(i).authorname);
                h.msg_time_r.setText(df.format(msgList.get(i).sendtime*1000));
                setAvatar(h.msg_avatar_r,avatarPath,msgList.get(i).authoravatar);
                h.chat_words_r.setText(msgList.get(i).body);
            }else{ //不是自己的消息
                h.send_text_right.setVisibility(View.GONE); //右边的隐藏
                h.msg_name.setText(msgList.get(i).authorname);
                h.msg_time.setText(df.format(msgList.get(i).sendtime*1000));
                setAvatar(h.msg_avatar,avatarPath,msgList.get(i).authoravatar);
                h.chat_words.setText(msgList.get(i).body);
            }
        }
        return view;
    }

    private View handlePhotoView(int i, View view, ViewGroup viewGroup){
        if(view==null){
            view=layoutInflater.inflate(R.layout.item_msg_img,viewGroup,false);
            view.setTag(new PhotoViewHolder(view));
        }
        if(view != null && view.getTag() instanceof PhotoViewHolder){
            PhotoViewHolder h=(PhotoViewHolder) view.getTag();
            String avatarPath= FileUtil.getFilePath(msgList.get(i).authoravatar);
            String filePath=FileUtil.getFilePath(msgList.get(i).body);
            if(msgList.get(i).ismine){ //如果是自己的消息
                h.send_photo_left.setVisibility(View.GONE); //左边的隐藏
                h.photo_name_r.setText(msgList.get(i).authorname);
                h.photo_time_r.setText(df.format(msgList.get(i).sendtime*1000));
                setAvatar(h.photo_avatar_r,avatarPath,msgList.get(i).authoravatar);
                setPhoto(h.chat_photo_r,filePath,msgList.get(i).body);
            }else{ //不是自己的消息
                h.send_photo_right.setVisibility(View.GONE); //左边的隐藏
                h.photo_name.setText(msgList.get(i).authorname);
                h.photo_time.setText(df.format(msgList.get(i).sendtime*1000));
                setAvatar(h.photo_avatar,avatarPath,msgList.get(i).authoravatar);
                setPhoto(h.chat_photo,filePath,msgList.get(i).body);
            }
        }
        return view;
    }

    private void setAvatar(NiceImageView imageView,String avatarPath,String avatarHash){
        if(avatarPath.equals("null")){ //如果没有存储过这个头像文件
            Textile.instance().ipfs.dataAtPath("/ipfs/" + avatarHash + "/0/small/content", new Handlers.DataHandler() {
                @Override
                public void onComplete(byte[] data, String media) {
                    String newPath=FileUtil.storeFile(data,avatarHash);
                    imageView.setImageBitmap(BitmapFactory.decodeFile(newPath));
                }
                @Override
                public void onError(Exception e) {
                }
            });
        }else{ //如果已经存储过这个头像
            imageView.setImageBitmap(BitmapFactory.decodeFile(avatarPath));
        }
    }

    private void setPhoto(ImageView imageView,String filePath,String fileHash){
        if(filePath.equals("null")){ //如果没有存储过图片
            Textile.instance().files.content(fileHash, new Handlers.DataHandler() {
                @Override
                public void onComplete(byte[] data, String media) {
                    String fileName=FileUtil.storeFile(data,fileHash);
                    imageView.setImageBitmap(BitmapFactory.decodeFile(fileName));
                }
                @Override
                public void onError(Exception e) {
                    imageView.setImageResource(R.drawable.ic_album);
                }
            });
        }else{
            imageView.setImageBitmap(BitmapFactory.decodeFile(filePath));
        }
    }
}
