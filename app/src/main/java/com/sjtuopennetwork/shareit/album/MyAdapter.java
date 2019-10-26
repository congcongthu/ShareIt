package com.sjtuopennetwork.shareit.album;


import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.sjtuopennetwork.shareit.R;

//创建Adapter
public class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {
    private static final String TAG = "MyAdapter";
    private String[] mDataSet;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    //创建ViewHolder
    public static class MyViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        View myView;
        ImageView imageView;
        private  final TextView textView;
        public MyViewHolder(View itemView) {
            super(itemView);


            myView=itemView;
            imageView=itemView.findViewById(R.id.iv_image);

            textView=itemView.findViewById(R.id.tv_text);
        }
        public TextView getTextView(){return  textView;}
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public MyAdapter(String[] myDataset) {
        mDataSet = myDataset;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public MyAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {


        // create a new view
        ImageView iv_image;
//        TextView v = (TextView) LayoutInflater.from(parent.getContext())
//                .inflate(R.layout.item_album, parent, false);
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_album, parent, false);
        //  ...
        MyViewHolder vh = new MyViewHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    //在这里操作item
    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        Log.d(TAG, "Element " + position + " set.");
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        //holder.imageView.setText(mDataset[position]);
        holder.getTextView().setText(mDataSet[position]);

        //这里可以设置item监听
        //对整个item监听
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Element " + holder.getAdapterPosition() + " clicked.");
            }
        });
        //对控件监听
//        holder.imageView.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Log.d(TAG, "Element " + holder.getAdapterPosition() + " clicked.");
//            }
//        });
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataSet.length;
    }
}

