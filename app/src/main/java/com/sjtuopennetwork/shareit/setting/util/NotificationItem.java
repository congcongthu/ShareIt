package com.sjtuopennetwork.shareit.setting.util;

public class NotificationItem {
    private int iId;
    private String iName;

    public String notiid;

    public String actor;

    public String body;

    public long sendTime;

    public boolean isRead;

    public String avatarPath;

    public NotificationItem() {

    }

    public NotificationItem(int iId, String iName) {
        this.iId = iId;
        this.iName = iName;
    }
    public NotificationItem(String notiid,String avatarPath,String actor, String body, long sendTime, boolean isRead) {
        this.notiid=notiid;
        this.actor = actor;
        this.body = body;
        this.sendTime = sendTime;
        this.isRead = isRead;
    }

    public int getiId() {
        return iId;
    }

    public String getiName() {
        return iName;
    }

    public void setiId(int iId) {
        this.iId = iId;
    }

    public void setiName(String iName) {
        this.iName = iName;
    }
}
