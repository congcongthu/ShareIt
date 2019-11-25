package com.sjtuopennetwork.shareit.contact.util;

import android.util.Pair;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import sjtu.opennet.textilepb.Model;
import sjtu.opennet.textilepb.View;
import sjtu.opennet.hon.Textile;

/**
 * Include some static methods which operate contacts or friends.
 */
public class ContactUtil {

    /**
     * Get the list of all friends.
     * If both the whitelist count and the peer count are 2, the peer is a friend.
     * If the whitelist count is 2 but the peer count is 1, it is a friend application which is still not accepted.
     * @return
     */
    public static List<Model.Peer> getFriendList(){
        List<Model.Peer> result=new LinkedList<>();
        List<Model.Thread> threads;
        try {
            threads=Textile.instance().threads.list().getItemsList();
            for(Model.Thread t:threads){
                if(t.getWhitelistCount()==2){ //如果是双人thread，并且的确有两个人
                    List<Model.Peer> peers=Textile.instance().threads.peers(t.getId()).getItemsList();
                    for(Model.Peer p:peers){
                        if(!p.getAddress().equals(Textile.instance().account.address())){ //不是自己就是好友
                            if(t.getKey().equals(p.getAddress()) && //thread的key等于这个peer的address，说明自己是申请者
                                    !Textile.instance().threads.isAdmin(t.getId(),p.getId())){ //如果好友不是管理员
                                Textile.instance().threads.addAdmin(t.getId(),p.getId()); //将其设为管理员
                            }
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

    /**
     * Get friend application and the corresponding applier.
     * If the name of an invite is "FriendThread1219", the invite is an friend application.
     * @return
     */
    public static Pair<List<View.InviteView>,List<ResultContact>> getApplication(){
        List<ResultContact> applications=new LinkedList<>();
        List<View.InviteView> friendApplications=new LinkedList<>();
        try {
            List<View.InviteView> invites = Textile.instance().invites.list().getItemsList();
            for (View.InviteView inviteView : invites) {
                if(inviteView.getName().equals("FriendThread1219")){ //找到好友申请的邀请
                    ResultContact resultContact=new ResultContact(inviteView.getInviter().getAddress(),inviteView.getInviter().getName(),inviteView.getInviter().getAvatar(),null,false);
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

    /**
     * Create a friend thread, called when sending a friend application to the contact.
     * The thread key is set to target address, and the whitelist includes the address of target user and applier.
     * @param targetAddress
     */
    public static void createTwoPersonThread(String targetAddress){
        sjtu.opennet.textilepb.View.AddThreadConfig.Schema schema=
                sjtu.opennet.textilepb.View.AddThreadConfig.Schema.newBuilder()
                        .setPreset(sjtu.opennet.textilepb.View.AddThreadConfig.Schema.Preset.MEDIA)
                        .build();
        sjtu.opennet.textilepb.View.AddThreadConfig config=sjtu.opennet.textilepb.View.AddThreadConfig.newBuilder()
                .setSharing(Model.Thread.Sharing.SHARED)
                .setType(Model.Thread.Type.OPEN)
                .setKey(targetAddress).setName("FriendThread1219")
                .addWhitelist(targetAddress).addWhitelist(Textile.instance().account.address()) //两个人添加到白名单
                .setSchema(schema)
                .build();
        try {
            Textile.instance().threads.add(config);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Create a group thread , called when creating a group.
     * The thread key is set to a random UUID string.
     * @param threadName
     */
    public static void createMultiPersonThread(String threadName){
        String key= UUID.randomUUID().toString();
        sjtu.opennet.textilepb.View.AddThreadConfig.Schema schema= sjtu.opennet.textilepb.View.AddThreadConfig.Schema.newBuilder()
                .setPreset(sjtu.opennet.textilepb.View.AddThreadConfig.Schema.Preset.MEDIA)
                .build();
        sjtu.opennet.textilepb.View.AddThreadConfig config=sjtu.opennet.textilepb.View.AddThreadConfig.newBuilder()
                .setSharing(Model.Thread.Sharing.SHARED)
                .setType(Model.Thread.Type.OPEN)
                .setKey(key).setName(threadName)
                .setSchema(schema)
                .build();
        try {
            Textile.instance().threads.add(config);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
