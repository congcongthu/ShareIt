package com.sjtuopennetwork.shareit.album.gmx;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.sjtuopennetwork.shareit.R;

import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder> {
    List <String> mFileName;
    List <String> mFileTime;
    List <String> mFilePath;
    String mFileImage;
    Context context;
    // Create new views (invoked by the layout manager)
    @Override
    public FileAdapter.FileViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_album_file, parent, false);

        FileAdapter.FileViewHolder vh = new FileAdapter.FileViewHolder(v);
        return vh;
    }

    public FileAdapter(Context context, String myFileImage, List<String> myFileName, List<String> myFileTime, List<String> myFilePath) {
        this.context=context;
        this.mFileImage=myFileImage;
        this.mFileName=myFileName;
        this.mFileTime=myFileTime;
        this.mFilePath=myFilePath;
    }
    public static class FileViewHolder extends RecyclerView.ViewHolder {
        View myView;
        ImageView imageView;
        TextView textView_name;
        TextView textView_time;
        TextView textView_path;

        public FileViewHolder(View v) {
            super(v);
            myView=v;
            imageView=v.findViewById(R.id.file_image);
            textView_name=v.findViewById(R.id.file_name);
            textView_time=v.findViewById(R.id.file_add_time);
            textView_path=v.findViewById(R.id.file_path);
        }

    }


    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
      //  holder.imageView=
     //   holder.textView_name=
       // holder.textView_time=
        System.out.println("===================num:  "+mFilePath.size());
        System.out.println("===================picdataset:  "+mFilePath.get(position));
        Bitmap bmp = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_pic_file);
        holder.imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        holder.imageView.setImageBitmap(bmp);
        holder.textView_name.setText(mFileName.get(position));
        holder.textView_time.setText(mFileTime.get(position));
      //  holder.textView_path.setText(mFilePath.get(position));
        holder.myView.setOnClickListener(v -> {
            System.out.println("======================Element:  "+holder.getAdapterPosition()+"   clicked");

            Intent it1=new Intent(context, FileShow.class);
            it1.putExtra("filename",mFileName.get(holder.getAdapterPosition()));
            it1.putExtra("filetime",mFileTime.get(holder.getAdapterPosition()));
            it1.putExtra("filepath",mFilePath.get(holder.getAdapterPosition()));
            context.startActivity(it1);
        });
    }

    @Override
    public int getItemCount() {
        return mFileName.size();
    }




}
