package com.sjtuopennetwork.shareit.album.gmx;


import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.sjtuopennetwork.shareit.R;

import java.util.List;




//创建Adapter
public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder> {
    List <String> mDataset;
    Context context;


    //创建ViewHolder
    public static class PhotoViewHolder extends RecyclerView.ViewHolder {
        View myView;
        ImageView imageView;
        TextView textView;
        public PhotoViewHolder(View v) {
            super(v);
            myView=v;
            imageView=v.findViewById(R.id.iv_image);
            textView=v.findViewById(R.id.tv_text);
        }
        public ImageView getImageView(){return  imageView;}


    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public PhotoAdapter(Context context,List<String> myPicDataset) {
        this.context=context;
        this.mDataset=myPicDataset;


    }


    // Create new views (invoked by the layout manager)
    @Override
    public PhotoAdapter.PhotoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {


        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_album_photo, parent, false);

        PhotoViewHolder vh = new PhotoViewHolder(v);
        return vh;
    }


    @Override
    public void onBindViewHolder(PhotoViewHolder holder, int position) {

        System.out.println("===================  mDataset.size(); "+ mDataset.size());
        System.out.println("===================picdataset:  "+mDataset.get(position));
        Bitmap bmp = BitmapFactory.decodeFile(mDataset.get(position));
        holder.imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        holder.imageView.setImageBitmap(bmp);



        //这里可以设置item监听
        holder.imageView.setOnClickListener(v -> {
            System.out.println("======================Element:  "+holder.getAdapterPosition()+"   clicked");

            Intent it1=new Intent(context, PhotoShow.class);
            it1.putExtra("imgpath", mDataset.get(holder.getAdapterPosition()));
            context.startActivity(it1);
        });
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.size();
    }



}

