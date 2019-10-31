package com.sjtuopennetwork.shareit.share.util;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.shehuan.niv.NiceImageView;
import com.sjtuopennetwork.shareit.R;
import com.sjtuopennetwork.shareit.util.FileUtil;

import java.util.List;

import sjtu.opennet.hon.Handlers;
import sjtu.opennet.hon.Textile;
import sjtu.opennet.textilepb.Model;

public class GroupMemberAdapter extends RecyclerView.Adapter<GroupMemberAdapter.ViewHolder> {

    private List<Model.Peer> members;
    Context context;

    public GroupMemberAdapter(Context context,List<Model.Peer> members) {
        this.context=context;
        this.members = members;
    }

    public static class ViewHolder extends  RecyclerView.ViewHolder {
        public NiceImageView avatar;
        public TextView name;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            avatar=itemView.findViewById(R.id.group_member_avatar);
            name=itemView.findViewById(R.id.group_member_name);
        }
    }


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view= LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_group_member,viewGroup,false);
        ViewHolder vh=new ViewHolder(view);
        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int i) {
//        viewHolder.avatar.setImageResource(R.drawable.ic_default_avatar);
        viewHolder.name.setText(members.get(i).getName());

        String avatarPath= FileUtil.getFilePath(members.get(i).getAvatar());
        setAvatar(viewHolder.avatar,avatarPath,members.get(i).getAvatar());
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    private void setAvatar(NiceImageView imageView,String avatarPath,String avatarHash){
        if(avatarPath.equals("null")){ //如果没有存储过这个头像文件
            Textile.instance().ipfs.dataAtPath("/ipfs/" + avatarHash + "/0/small/content", new Handlers.DataHandler() {
                @Override
                public void onComplete(byte[] data, String media) {
                    String newPath= FileUtil.storeFile(data,avatarHash);
                    imageView.setImageBitmap(BitmapFactory.decodeFile(newPath));
                }
                @Override
                public void onError(Exception e) {
                }
            });
        }else{ //如果已经存储过这个头像
//            imageView.setImageBitmap(BitmapFactory.decodeFile(avatarPath));
            Glide.with(context).load(avatarPath).thumbnail(0.3f).into(imageView);
        }
    }
}
