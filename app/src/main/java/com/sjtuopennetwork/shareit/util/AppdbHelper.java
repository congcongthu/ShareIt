package com.sjtuopennetwork.shareit.util;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class AppdbHelper extends SQLiteOpenHelper {

    //对话表
    private String CREATE_DIALOGS="create table dialogs " +
            "(id integer primary key autoincrement," +
            "threadid text," +
            "threadname text," +
            "lastmsg text," +
            "lastmsgdate integer," +
            "isread integer," +
            "imgpath text,"+ //存放图片的hash值，在adapter中加载图片
            "issingle integer,"+ //1为双人thread，0为多人thread
            "isvisible integer)"; //1为可见，0为不可见。双人thread不能被删除，只能是设置为不可见

    //消息表
    private String CREATE_MSGS="create table msgs"+
            "(id integer primary key autoincrement," +
            "threadid text," +
            "msgtype integer," +  //0文本、1图片、2视频
            "blockid text," +  //评论和点赞是直接对block添加，需要blockid
            "authorname text,"+ //作者昵称
            "authoravatar text," +  //作者头像
            "body text," + //内容，文本消息就是消息，图片消息是图片的hash值（adapter中加载图片）
            "sendtime integer," +
            "ismine integer)"; //1表示是我的消息，0为别人消息

    public AppdbHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(CREATE_DIALOGS);
        sqLiteDatabase.execSQL(CREATE_MSGS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }
}
