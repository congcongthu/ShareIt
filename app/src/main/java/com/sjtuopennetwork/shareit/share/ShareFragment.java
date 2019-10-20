package com.sjtuopennetwork.shareit.share;


import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.sjtuopennetwork.shareit.R;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import razerdp.basepopup.BasePopupWindow;

/**
 * A simple {@link Fragment} subclass.
 */
public class ShareFragment extends Fragment {

    //UI控件
    DialogAdapter dialogAdapter;  //对话列表的数据适配器
    ListView dialoglistView; //对话列表
    ImageView bt_share_menu; //右上角加号按钮


    //内存数据
    List<TDialog> dialogs; //对话列表数据


    public ShareFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_share, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();

        initUI();

        initData();

    }

    private void initUI(){
        dialoglistView=getActivity().findViewById(R.id.dialogs_lv);
        //右上角菜单按钮
        bt_share_menu=getActivity().findViewById(R.id.bt_share_menu);
        bt_share_menu.setOnClickListener(v -> {
            BtShareMenu btShareMenu=new BtShareMenu(getContext());
            btShareMenu.setBackgroundColor(Color.parseColor("#00000000"));

            btShareMenu.showPopupWindow(v);
        });
    }

    private void initData(){
        dialogs=new LinkedList<>();

        //这些数据应该是从数据库中查出来
        dialogs.add(new TDialog(1,"1","实验室","你好呀",1546300,false,"null",false,true));
        dialogs.add(new TDialog(2,"2","教室","你好呀",1546300,false,"null",false,true));
        dialogs.add(new TDialog(3,"3","老王","你好呀",1546300,false,"null",true,true));
        for(int i=0;i<10;i++){
            TDialog t1=new TDialog(1,"1","测试"+i,"你好呀",1546300,false,"null",false,true);
            dialogs.add(t1);
        }
        dialogs.add(0,new TDialog(1,"1","通知","你好呀",1546300,false,"tongzhi",false,true));

        dialogAdapter=new DialogAdapter(getContext(),R.layout.item_share_dialog,dialogs);
        dialoglistView.setAdapter(dialogAdapter);
    }

    //右上角菜单
    class BtShareMenu extends BasePopupWindow{
        LinearLayout create_view;
        LinearLayout qrcode_join_gorup;
        Context context;

        public BtShareMenu(Context context) {
            super(context);
            this.context=context;
            create_view=findViewById(R.id.create_group);
        }
        @Override
        public View onCreateContentView() {
            View view= createPopupById(R.layout.pop_share_add_menu);
            LinearLayout create_gp=view.findViewById(R.id.create_group);
            create_gp.setOnClickListener(v -> {

            });
            return view;
        }

//        //先不加动画
//        @Override
//        protected Animator onCreateShowAnimator() {
//            return createAnimator(true);
//        }
//
//        @Override
//        protected Animator onCreateDismissAnimator() {
//            return createAnimator(false);
//        }
//
//        private Animator createAnimator(boolean isShow) {
//            ObjectAnimator objectAnimator=ObjectAnimator.ofFloat(getDisplayAnimateView(),
//                    View.TRANSLATION_Y,
//                    isShow ? 0:0,
//                    isShow ? 0:-300);
//            objectAnimator.setDuration(500);
//            objectAnimator.setInterpolator(new OvershootInterpolator(isShow ? 6 : -6));
//            return objectAnimator;
//        }
    }

}
