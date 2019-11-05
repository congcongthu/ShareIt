package com.sjtuopennetwork.shareit.album;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.sjtuopennetwork.shareit.R;

import java.io.File;
import java.util.List;

//创建Adapter
public class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {
    private static final String TAG = "MyAdapter";
    List <String> mDataset;
    //private  String picDataset;



    //创建ViewHolder
    public static class MyViewHolder extends RecyclerView.ViewHolder {
        View myView;
        ImageView imageView;
        TextView textView;
        public MyViewHolder(View v) {
            super(v);
            myView=v;
            imageView=v.findViewById(R.id.iv_image);
            textView=v.findViewById(R.id.tv_text);
        }
        public TextView getTextView(){return  textView;}
        public ImageView getImageView(){return  imageView;}


    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public MyAdapter(List myPicDataset) {
        // mDataSet = myDataset;
        mDataset=myPicDataset;
    }


    // Create new views (invoked by the layout manager)
    @Override
    public MyAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {


        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_album, parent, false);

        MyViewHolder vh = new MyViewHolder(v);
        return vh;
    }


    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {

        Log.d(TAG, "Element " + position + " set.");

        // holder.getTextView().setText(mDataSet[position]);

        System.out.println("===================picdataset:  "+mDataset.get(position));
        Bitmap bmp = BitmapFactory.decodeFile(mDataset.get(position));
        holder.imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        holder.imageView.setImageBitmap(bmp);



        //这里可以设置item监听
        //对整个item监听
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println("======================Element:  "+holder.getAdapterPosition()+"   clicked");
                Log.d(TAG, "Element " + holder.getAdapterPosition() + " clicked.");
            }
        });
        // 对控件监听
        holder.imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Element " + holder.getAdapterPosition() + " clicked.");
            }
        });
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.size();
    }
}

