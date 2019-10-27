package com.sjtuopennetwork.shareit.contact.util;

import android.util.Pair;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import io.textile.pb.Model;
import io.textile.pb.View;
import io.textile.textile.Textile;

public class GetFriendListOrApplication {

    public static List<Model.Peer> getFriendList(){
        List<Model.Peer> result=new LinkedList<>();
        List<Model.Thread> threads;

        try {
            threads=Textile.instance().threads.list().getItemsList();
            System.out.println("==============thread数量"+threads.size());
            for(Model.Thread t:threads){
                //后面要改成thread的peer能够得到自己
                if(t.getWhitelistCount()==2){ //如果是双人thread，并且的确有两个人
                    List<Model.Peer> peers=Textile.instance().threads.peers(t.getId()).getItemsList();
                    System.out.println("=========白名单为2的thread及其peer数："+t.getName()+" "+t.getId()+" "+peers.size());
                    for(Model.Peer p:peers){
                        System.out.println("==============peer:"+p.getName());
                        if(!p.getAddress().equals(Textile.instance().account.address())){ //不是自己就是好友
                            result.add(p);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static Pair<List<View.InviteView>,List<ResultContact>> getApplication(){
        List<ResultContact> applications=new LinkedList<>();
        List<View.InviteView> friendApplications=new LinkedList<>();
        try {
            List<View.InviteView> invites = Textile.instance().invites.list().getItemsList();
            System.out.println("=============invite数量："+invites.size());
            for (View.InviteView inviteView : invites) {
                String friendaddress = inviteView.getInviter().getAddress();
                boolean isApplication = true;
                for (Model.Contact c : Textile.instance().contacts.list().getItemsList()) {
                    if (c.getAddress().equals(friendaddress)) { //如果查到是好友发来的通知就不添加
                        isApplication = false;
                        break;
                    }
                }
                if (isApplication) {
                    ResultContact resultContact=new ResultContact(friendaddress,inviteView.getInviter().getName(),inviteView.getInviter().getAvatar(),null);
                    applications.add(resultContact);
                    friendApplications.add(inviteView);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        Pair<List<View.InviteView>,List<ResultContact>> result=new Pair<>(friendApplications,applications);
        return result;
    }

}
