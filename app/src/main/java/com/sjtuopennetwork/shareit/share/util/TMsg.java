package com.sjtuopennetwork.shareit.share.util;

public class TMsg {
    public int id;
    public String threadid;
    public int msgtype;
    public String blockid;
    public String authorname;
    public String authoravatar;
    public String body;
    public long sendtime;
    boolean ismine;

    public TMsg(int id, String threadid, int msgtype, String blockid, String authorname, String authoravatar, String body, long sendtime, boolean ismine) {
        this.id = id;
        this.threadid = threadid;
        this.msgtype = msgtype;
        this.blockid = blockid;
        this.authorname = authorname;
        this.authoravatar = authoravatar;
        this.body = body;
        this.sendtime = sendtime;
        this.ismine = ismine;
    }
}
