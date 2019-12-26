package com.sjtuopennetwork.shareit.util;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class AppdbHelper extends SQLiteOpenHelper {
    private static AppdbHelper helperInstance=null;
    public synchronized static AppdbHelper getInstance(Context context,String dbname){
        if(helperInstance==null){
            helperInstance=new AppdbHelper(context,dbname);
        }
        return helperInstance;
    }

    public AppdbHelper(Context context,String dbname){
        super(context,dbname,null,1);
    }

    public static void setNull(){
        helperInstance.close();
        helperInstance=null;
    }

    //对话表
    private String CREATE_DIALOGS="create table dialogs " +
            "(id integer primary key autoincrement," +
            "threadid text," +
            "threadname text," +
            "lastmsg text," +
            "lastmsgdate integer," +
            "isread integer," +
            "imgpath text,"+ //存放图片的路径，加载失败就用默认图片
            "issingle integer,"+ //1为双人thread，0为多人thread
            "isvisible integer)"; //1为可见，0为不可见。双人thread不能被删除，只能是设置为不可见

    //消息表
    private String CREATE_MSGS="create table msgs"+
//            "(id integer primary key autoincrement," +
            "(blockid text primary key," +  //评论和点赞是直接对block添加，需要blockid
            "threadid text," +
            "msgtype integer," +  //0文本、1图片、2视频
            "authorname text,"+ //作者昵称
            "authoravatar text," +  //作者头像
            "body text," + //内容，文本消息就是消息，图片消息是图片的路径，视频消息是视频的缩略图拼视频的VideoID
            "sendtime integer," +
            "ismine integer)"; //1表示是我的消息，0为别人消息
    //文件表
    private String CREATE_FILES="create table files"+
            "(id integer primary key autoincrement," +
            "filename text," +
            "filetime text," +
            "filepath text)";

    //图片表
    private String CREATE_PHOTO="create table photo"+
            "(id integer primary key autoincrement," +
            "photoname text,"+
            "phototime text,"+
            "photopath text,"+
            "photodata text,"+
            "isdele integer)";
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(CREATE_DIALOGS);
        sqLiteDatabase.execSQL(CREATE_MSGS);
        sqLiteDatabase.execSQL(CREATE_FILES);
        sqLiteDatabase.execSQL(CREATE_PHOTO);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }
}
