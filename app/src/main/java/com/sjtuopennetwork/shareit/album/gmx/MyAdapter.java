package com.sjtuopennetwork.shareit.album.gmx;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.luck.picture.lib.photoview.PhotoView;
import com.sjtuopennetwork.shareit.R;

import java.util.List;

//创建Adapter
public class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {
    private static final String TAG = "MyAdapter";
    List <String> mDataset;
    // List<String> largeHash=new ArrayList<String>();
    PhotoView mPhotoView;
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
    // Provide a suitable constructor (depends on the kind of dataset)
//    public MyAdapter() {
//
//    }

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
        holder.imageView.setOnClickListener(v -> {
            System.out.println("======================Element:  "+holder.getAdapterPosition()+"   clicked");
            // Log.d(TAG, "Element " + holder.getAdapterPosition() + " clicked.");
            String photopath = mDataset.get(holder.getAdapterPosition());
            //  Intent it = new Intent(MyAdapter.class,PhotoShow.class);
            // startActivity(it);

        });
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    //得到photo thread中的所有hash
    //将hash转为本地路径
    //设置适配器
    /*
    private  void initData(String threadId){
            mDataset.clear();
            largeHash.clear();
            int listnum = 0;
            try {
                listnum = Textile.instance().files.list(threadId, "", 256).getItemsCount();
            } catch (Exception e) {
                e.printStackTrace();
            }

            // System.out.println("===============Item个数："+listnum);
            //得到photo thread中所有hash
            for (int i = 0; i < listnum; i++) {
                String large_hash = "";
                try {

                    large_hash = Textile.instance().files.list(threadId, "", listnum).getItems(i).getFiles(0).getLinksMap().get("large").getHash();
                    //  choosePic=Textile.instance().files.list(threadId,"",listnum);
                    System.out.println("===================================photo_thread hash" + i + ":   " + large_hash);

                } catch (Exception e) {
                    e.printStackTrace();
                }
                //排除相同hash，即相同的图片
                //    if(!largeHash.contains(large_hash)){
                largeHash.add(large_hash);
                //   }

            }
            for (int i = 0; i < largeHash.size(); i++) {
                int finalI = i;
                String filepath = FileUtil.getFilePath(largeHash.get(finalI));
                if (filepath.equals("null")) {
                    Textile.instance().files.content(largeHash.get(i), new Handlers.DataHandler() {
                        @Override
                        public void onComplete(byte[] data, String media) {
                            //存储下来的包括路径的完整文件名
                            picDataset = FileUtil.storeFile(data, largeHash.get(finalI));
                            System.out.println("=========================文件不存在取得 " + picDataset);
                            mDataset.add(picDataset);
                        }

                        @Override
                        public void onError(Exception e) {
                        }
                    });
                } else {
                    mDataset.add(filepath);
                }

            }
            //
            for (int i = 0; i < mDataset.size(); i++) {
                if (mDataset.get(i).equals(null)) {
                    mDataset.remove(i);
                }
            }
            Collections.reverse(mDataset);
    }

     */

}

