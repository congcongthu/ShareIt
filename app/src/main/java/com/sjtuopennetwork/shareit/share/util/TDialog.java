package com.sjtuopennetwork.shareit.share.util;

public class TDialog {
    public int id;
    public String threadid;
    public String threadname;
    public String lastmsg;
    public long lastmsgdate;
    public boolean isRead;
    public String imgpath;
    public boolean isSingle; //标记是否是两人的thread
    public boolean isVisible; //如果是两人的thread，删除只是设置不可见，下一次发消息之后再次出现

    public TDialog(int id, String threadid, String threadname, String lastmsg, long lastmsgdate, boolean isRead, String imgpath, boolean isSingle, boolean isVisible) {
        this.id = id;
        this.threadid = threadid;
        this.threadname = threadname;
        this.lastmsg = lastmsg;
        this.lastmsgdate = lastmsgdate;
        this.isRead = isRead;
        this.imgpath = imgpath;
        this.isSingle = isSingle;
        this.isVisible = isVisible;
    }

}
