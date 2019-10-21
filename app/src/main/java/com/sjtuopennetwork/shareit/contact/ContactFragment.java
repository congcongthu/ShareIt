package com.sjtuopennetwork.shareit.contact;


import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.chezi008.libcontacts.bean.ContactBean;
import com.chezi008.libcontacts.widget.ContactView;
import com.sjtuopennetwork.shareit.R;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import io.textile.pb.Model;

/**
 * A simple {@link Fragment} subclass.
 */
public class ContactFragment extends Fragment {

    //UI控件
    private ContactView contactView;
    LinearLayout contact_discover_layout;
    LinearLayout new_friend_layout;
    ImageView bt_contact_search;

    //内存数据
    List<Model.Contact> contacts;


    public ContactFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_contact, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();

        initUI();

        initData();

        contactView=getActivity().findViewById(R.id.contact_list);
        List<ContactBean> contactBeans=new LinkedList<>();
        for(int i=0;i<3;i++){
            ContactBean contactBean=new ContactBean();
            contactBean.setName("赵"+i);
            contactBean.setAvatar("null");
            contactBeans.add(contactBean);
        }
        for(int i=0;i<3;i++){
            ContactBean contactBean=new ContactBean();
            contactBean.setName("a"+i);
            contactBean.setAvatar("null");
            contactBeans.add(contactBean);
        }
        for(int i=0;i<3;i++){
            ContactBean contactBean=new ContactBean();
            contactBean.setName("李"+i);
            contactBean.setAvatar("null");
            contactBeans.add(contactBean);
        }
        for(int i=0;i<3;i++){
            ContactBean contactBean=new ContactBean();
            contactBean.setName("L"+i);
            contactBean.setAvatar("null");
            contactBeans.add(contactBean);
        }
        contactView.setData(contactBeans,false);
    }

    private void initData(){

    }

    private void initUI() {
        contact_discover_layout=getActivity().findViewById(R.id.contact_discover_layout);
        new_friend_layout=getActivity().findViewById(R.id.contact_new_friend_layout);
        bt_contact_search=getActivity().findViewById(R.id.bt_contact_search);

        contact_discover_layout.setOnClickListener(v -> {
            Intent it=new Intent(getActivity(),ContactDiscoverActivity.class);
            startActivity(it);
        });
        new_friend_layout.setOnClickListener(v -> {
            Intent it=new Intent(getActivity(),NewFriendActivity.class);
            startActivity(it);
        });
        bt_contact_search.setOnClickListener(v -> {
            Intent it=new Intent(getActivity(),SearchContactActivity.class);
            startActivity(it);
        });
    }
}
