package com.sjtuopennetwork.shareit.album.util;

import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.sjtuopennetwork.shareit.R;
import com.sjtuopennetwork.shareit.share.ImageInfoActivity;

import java.util.ArrayList;

public class PhotoAdapter extends RecyclerView.Adapter {

    Context ctx;
    ArrayList<byte[]> photoBytes;

    public PhotoAdapter(Context context, ArrayList<byte[]> photoBytes) {
        this.ctx=context;
        this.photoBytes = photoBytes;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView photoImage;
        View photoView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            photoView=itemView;
            photoImage=itemView.findViewById(R.id.sync_photo_image);
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view= LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_sync_photo,viewGroup,false);
        ViewHolder viewHolder=new ViewHolder(view);
        viewHolder.photoView.setOnClickListener(v -> {
            int position=viewHolder.getAdapterPosition();
            Bundle b=new Bundle();
            b.putByteArray("photoData",photoBytes.get(position));
            Intent it=new Intent(ctx, ImageInfoActivity.class);
            it.putExtras(b);
            ctx.startActivity(it);
        });
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        byte[] thePhoto=photoBytes.get(position);
        ViewHolder myViewHolder=(ViewHolder)viewHolder;
        myViewHolder.photoImage.setImageBitmap(BitmapFactory.decodeByteArray(thePhoto,0,thePhoto.length));
    }

    @Override
    public int getItemCount() {
        return photoBytes.size();
    }
}
