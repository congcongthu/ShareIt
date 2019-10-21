package com.sjtuopennetwork.shareit.contact;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.SearchView;

import com.sjtuopennetwork.shareit.R;
import com.sjtuopennetwork.shareit.util.FileUtil;
import com.sjtuopennetwork.shareit.util.MyEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.LinkedList;
import java.util.List;

import io.textile.pb.Model;
import io.textile.pb.QueryOuterClass;
import io.textile.textile.Handlers;
import io.textile.textile.Textile;

public class SearchContactActivity extends AppCompatActivity {

    //UI控件
    SearchView searchView;
    ListView contact_search_result_lv;
    SearchResultAdapter searchResultAdapter;

    //内存数据
    List<Model.Contact> contacts;
    List<SearchResultContact> resultContacts;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_contact);

        initUI();

        initData();

        if(!EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().register(this);
        }
    }

    private void initData() {
        contacts=new LinkedList<>();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                contacts.clear(); //清空结果列表
                resultContacts.clear();
                QueryOuterClass.QueryOptions options = QueryOuterClass.QueryOptions.newBuilder()
                        .setWait(10)
                        .setLimit(1)
                        .build();
                QueryOuterClass.ContactQuery query = QueryOuterClass.ContactQuery.newBuilder()
                        .setName(s)
                        .build();
                try {
                    Textile.instance().contacts.search(query, options);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return true;
            }
        });


    }

    private void initUI() {
        searchView=findViewById(R.id.contact_search);
        contact_search_result_lv=findViewById(R.id.contact_search_result_lv);

        resultContacts=new LinkedList<>();
    }

    //每得到一个搜索结果就会调用一次
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void showSearch(MyEvent event){
        if(event.getCode()!=1){
            return;
        }
        Model.Contact c=(Model.Contact) event.getEvent();
        System.out.println("=============得到搜索结果："+c.getName());

        //这里不区分是否是已添加的联系人，直到点到详情里面才根据不同的用户显示不同的布局
        contacts.add(c);
        String addr=c.getAddress();
        String addr_last10="address: "+addr.substring(addr.length()-10);
        if(c.getAvatar().equals("")){ //如果是没有设置头像，就直接添加进去
            System.out.println("=======没有设置头像"+c.getName());
            resultContacts.add(new SearchResultContact(addr_last10,c.getName(),null,"null"));
        }else{ //如果是设置过头像，要看是否存储过
            System.out.println("========头像hash："+c.getAvatar());
            String avatarPath= FileUtil.getFilePath(c.getAvatar());
            if(avatarPath.equals("null")){ //如果没有存储这个用户的头像，就使用网络上的数据
                String avatarHash=c.getAvatar();
                Textile.instance().ipfs.dataAtPath("/ipfs/" + avatarHash + "/0/small/content", new Handlers.DataHandler() {
                    @Override
                    public void onComplete(byte[] data, String media) {
                        System.out.println("=================设置了头像"+c.getName());
                        resultContacts.add(new SearchResultContact(addr_last10,c.getName(),data,"null"));
                        drawSearchResult();
                    }
                    @Override
                    public void onError(Exception e) {
                    }
                });
            }else{ //如果已经存储了这个用户，就直接添加到结果列表显示出来
                System.out.println("=========已经存储过"+c.getName());
                resultContacts.add(new SearchResultContact(addr_last10,c.getName(),null,avatarPath));
            }
        }
        drawSearchResult();
    }

    private void drawSearchResult() {
        System.out.println("=========长度："+resultContacts.size());
        List<SearchResultContact> newResult=new LinkedList<>();
        for(SearchResultContact s:resultContacts){
            newResult.add(s);
        }
        searchResultAdapter=new SearchResultAdapter(this,R.layout.item_contact_search_result,newResult);
        contact_search_result_lv.setAdapter(searchResultAdapter);
    }
}
