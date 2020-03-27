package com.sjtuopennetwork.shareit.util;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.sjtuopennetwork.shareit.share.util.TDialog;
import com.sjtuopennetwork.shareit.share.util.TMsg;

import java.util.LinkedList;
import java.util.List;

public class DBHelper extends SQLiteOpenHelper {

    private static DBHelper helperInstance=null;
    private static SQLiteDatabase appdb;

    public DBHelper(Context context,String dbname){
        super(context,dbname,null,1);
    }

    public static void setNull(){
        appdb.close();
        appdb=null;
        helperInstance.close();
        helperInstance=null;

    }

    public synchronized static DBHelper getInstance(Context context,String dbname){
        if(helperInstance == null){
            helperInstance=new DBHelper(context,dbname);
            appdb=helperInstance.getWritableDatabase();
        }
        return helperInstance;
    }

    //对话表，用来存放thread的补充信息
    private String CREATE_DIALOGS="create table dialogs " +
            "(threadid text primary key ," + //对话的id，可以先列出所有的thread，然后查数据库获得相应的补充信息
            "lastmsg text," +
            "lastmsgdate integer," +
            "isread integer," +
            "add_or_img text,"+ //双人对话存对方address，多人群组存上一次的图片
            "issingle integer,"+ //1为双人thread，0为多人thread
            "isvisible integer)"; //1为可见，0为不可见。双人thread不能被删除，只能是设置为不可见

    //消息表
    private String CREATE_MSGS="create table msgs"+
            "(blockid text primary key," +  //评论和点赞是直接对block添加，需要blockid
            "threadid text," +
            "msgtype integer," +  //0文本、1图片、2视频、3文件
            "author text,"+ //作者address，用来获取昵称和头像
            "body text," + //内容，文本消息就是消息，图片消息是图片的路径，视频消息是视频的缩略图拼视频的VideoID
            "sendtime integer," +
            "ismine integer)"; //1表示是我的消息，0为别人消息，因为前端显示要分左右

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(CREATE_DIALOGS);
        sqLiteDatabase.execSQL(CREATE_MSGS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public synchronized List<TMsg> list3000Msg(String threadId){
        List<TMsg> msgs=new LinkedList<>();
        Cursor cursor=appdb.rawQuery("select * from msgs where threadid = ? order by sendtime desc limit 3000 offset 0",new String[]{threadId});
        if(cursor.moveToFirst()){
            do{
                int msgtype=cursor.getInt(cursor.getColumnIndex("msgtype"));
                String blockid=cursor.getString(cursor.getColumnIndex("blockid"));
                String author=cursor.getString(cursor.getColumnIndex("author"));
                String body=cursor.getString(cursor.getColumnIndex("body"));
                long sendtime=cursor.getLong(cursor.getColumnIndex("sendtime"));
                int ismine=cursor.getInt(cursor.getColumnIndex("ismine"));

                TMsg tMsg=new TMsg(blockid,threadId,msgtype,author,body,sendtime,ismine==1);
                msgs.add(0,tMsg); //从数据库是倒着排序搜索出来的，所以插到头部，后面就能够正确顺序显示
            }while (cursor.moveToNext());
        }
        cursor.close();
        return msgs;
    }

    public synchronized List<TDialog> queryAllDIalogs(){
        List<TDialog> dialogs=new LinkedList<>();
        Cursor cursor=appdb.query("dialogs",new String[]{"threadid","lastmsg","lastmsgdate","isread","add_or_img","issingle"},
                "isvisible=?",new String[]{"1"},null,null,"lastmsgdate desc");
        if(cursor.moveToFirst()){
            do{
                String threadid=cursor.getString(cursor.getColumnIndex("threadid"));
                String lastmsg=cursor.getString(cursor.getColumnIndex("lastmsg"));
                long lastmsgdate=cursor.getLong(cursor.getColumnIndex("lastmsgdate"));
                boolean isread=cursor.getInt(cursor.getColumnIndex("isread"))==1;
                String add_or_img=cursor.getString(cursor.getColumnIndex("add_or_img"));
                boolean isSingle=cursor.getInt(cursor.getColumnIndex("issingle"))==1;

                TDialog tDialog=new TDialog(threadid,lastmsg,lastmsgdate,isread,add_or_img,isSingle,true);
                dialogs.add(tDialog);
            }while (cursor.moveToNext());
        }
        cursor.close();
        return dialogs;
    }

    public synchronized  void changeDialogRead(String threadid, int isread){
        ContentValues v=new ContentValues();
        v.put("isread",isread);
        appdb.beginTransaction();
        appdb.update("dialogs",v,"threadid=?",new String[]{threadid});
        appdb.setTransactionSuccessful();
        appdb.endTransaction();
    }

    public synchronized  TMsg insertMsg(String threadid, int msgtype, String blockid, String author,
                                 String body, long sendtime, int ismine){
        ContentValues v=new ContentValues();
        v.put("threadid",threadid);
        v.put("msgtype",msgtype);
        v.put("blockid",blockid);
        v.put("author",author);
        v.put("body",body);
        v.put("sendtime",sendtime);
        v.put("ismine",ismine);
        appdb.beginTransaction();
        appdb.insertOrThrow("msgs",null,v);
        appdb.setTransactionSuccessful();
        appdb.endTransaction();

        return new TMsg(blockid,threadid,msgtype,author,body,sendtime,ismine==1);
    }

    //得到新消息时，对话要更新的内容可能有：图片、内容、时间
    public synchronized TDialog dialogGetMsg(TDialog tDialog,String threadId,String lastmsg,long lastmsgdate,String add_or_img){
        ContentValues v=new ContentValues();
        v.put("lastmsg",lastmsg); tDialog.lastmsg=lastmsg;
        v.put("lastmsgdate",lastmsgdate); tDialog.lastmsgdate=lastmsgdate;
        v.put("add_or_img",add_or_img); tDialog.add_or_img=add_or_img;
        v.put("isread",0);
        appdb.update("dialogs",v,"threadid=?",new String[]{threadId});
        return tDialog;
    }

    public synchronized TDialog insertDialog(String threadid, String lastmsg, long lastmsgdate, int isRead, String add_or_img, int isSingle, int isVisible){
        ContentValues v=new ContentValues();
        v.put("threadid",threadid);
        v.put("lastmsg",lastmsg);
        v.put("lastmsgdate",lastmsgdate);
        v.put("isread",isRead);
        v.put("add_or_img",add_or_img);
        v.put("issingle",isSingle);
        v.put("isvisible",isVisible);
        appdb.beginTransaction();
        appdb.insert("dialogs",null,v);
        appdb.setTransactionSuccessful();
        appdb.endTransaction();
        return new TDialog(threadid,lastmsg,lastmsgdate,isRead==1,add_or_img,isSingle==1,isVisible==1);
    }

    public synchronized  TDialog queryDialogByThreadID(String threadId){
        Cursor cursor=appdb.query("dialogs", null,"threadid=?",new String[]{threadId},null,null,null);
        if(cursor.moveToFirst()){
            String threadid=cursor.getString(cursor.getColumnIndex("threadid"));
            String lastmsg=cursor.getString(cursor.getColumnIndex("lastmsg"));
            long lastmsgdate=cursor.getLong(cursor.getColumnIndex("lastmsgdate"));
            boolean isRead=cursor.getInt(cursor.getColumnIndex("isread"))==1;
            String add_or_img=cursor.getString(cursor.getColumnIndex("add_or_img"));
            boolean isSingle=cursor.getInt(cursor.getColumnIndex("issingle"))==1;
            boolean isVisible=cursor.getInt(cursor.getColumnIndex("isvisible"))==1;

            TDialog result=new TDialog(threadid,lastmsg,lastmsgdate, isRead, add_or_img, isSingle, isVisible);
            cursor.close();
            return result;
        }else{
            cursor.close();
            return null;
        }
    }
}
