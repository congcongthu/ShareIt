package com.sjtuopennetwork.shareit.contact.util;

import java.util.LinkedList;
import java.util.List;

import io.textile.pb.Model;
import io.textile.textile.Textile;

public class GetFriendList {

    public static List<Model.Peer> get(){
        List<Model.Peer> result=new LinkedList<>();
        List<Model.Thread> threads;

        try {
            threads=Textile.instance().threads.list().getItemsList();
            for(Model.Thread t:threads){
                //后面要改成thread的peer能够得到自己
                if(t.getWhitelistCount()==2 && t.getPeerCount()==1){ //如果是双人thread，并且的确有两个人
                    List<Model.Peer> peers=Textile.instance().threads.peers(t.getId()).getItemsList();
                    for(Model.Peer p:peers){
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

}
