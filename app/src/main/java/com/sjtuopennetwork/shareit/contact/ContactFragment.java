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
import com.chezi008.libcontacts.listener.ContactListener;
import com.chezi008.libcontacts.widget.ContactView;
import com.example.qrlibrary.qrcode.utils.PermissionUtils;
import com.sjtuopennetwork.shareit.R;
import com.sjtuopennetwork.shareit.util.FileUtil;

import java.util.LinkedList;
import java.util.List;

import io.textile.pb.Model;
import io.textile.textile.Textile;

/**
 * A simple {@link Fragment} subclass.
 */
public class ContactFragment extends Fragment {

    //UI控件
    private ContactView contactView;
    LinearLayout contact_discover_layout;
    LinearLayout new_friend_layout;
    ImageView bt_contact_search;
    ImageView bt_contact_scan;

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



    }

    private void initData(){
        List<ContactBean> contactBeans=new LinkedList<>();
        try {
            List<Model.Contact> contacts= Textile.instance().contacts.list().getItemsList();
            for(Model.Contact c:contacts){
                ContactBean contactBean=new ContactBean();
                contactBean.setId(c.getAddress());
                contactBean.setName(c.getName());
                String avatarPath= FileUtil.getFilePath(c.getAvatar());
                contactBean.setAvatar(avatarPath);
                contactBeans.add(contactBean);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        contactView.setData(contactBeans,false);
        contactView.setContactListener(new ContactListener<ContactBean>() {
            @Override
            public void onClick(ContactBean item) {
                Intent it=new Intent(getActivity(),ContactInfoActivity.class);
                it.putExtra("address",item.getId());
                startActivity(it);
            }

            @Override
            public void onLongClick(ContactBean item) {

            }

            @Override
            public void loadAvatar(ImageView imageView, String avatar) {

            }
        });
    }

    private void initUI() {
        contact_discover_layout=getActivity().findViewById(R.id.contact_discover_layout);
        new_friend_layout=getActivity().findViewById(R.id.contact_new_friend_layout);
        bt_contact_search=getActivity().findViewById(R.id.bt_contact_search);
        contactView=getActivity().findViewById(R.id.contact_list);
        bt_contact_scan=getActivity().findViewById(R.id.bt_contact_scan);

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
        bt_contact_scan.setOnClickListener(v -> {
            PermissionUtils.getInstance().requestPermission(getActivity());
            Intent it=new Intent(getActivity(),ContactQRCodeAtivity.class);
            startActivity(it);
        });
    }
}
