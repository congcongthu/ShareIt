package com.sjtuopennetwork.shareit.util;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.sjtuopennetwork.shareit.share.util.TDialog;
import com.sjtuopennetwork.shareit.share.util.TMsg;

import java.util.LinkedList;
import java.util.List;

public class DBoperator {

    private static final String TAG = "================";

    DBoperator dBoperatorInstance;

    public static List<TMsg> queryMsg(SQLiteDatabase appdb,String threadId){
        List<TMsg> msgs=new LinkedList<>();
        Cursor cursor=appdb.rawQuery("select * from msgs where threadid = ? order by sendtime desc limit 3000 offset 0",new String[]{threadId});
        if(cursor.moveToFirst()){
            do{
                int msgtype=cursor.getInt(cursor.getColumnIndex("msgtype"));
                String blockid=cursor.getString(cursor.getColumnIndex("blockid"));
                String authorname=cursor.getString(cursor.getColumnIndex("authorname"));
                String authoravatar=cursor.getString(cursor.getColumnIndex("authoravatar"));
                String body=cursor.getString(cursor.getColumnIndex("body"));
                long sendtime=cursor.getLong(cursor.getColumnIndex("sendtime"));
                int ismine=cursor.getInt(cursor.getColumnIndex("ismine"));

                TMsg tMsg=new TMsg(blockid,threadId,msgtype,authorname,authoravatar,body,sendtime,ismine==1);
                msgs.add(0,tMsg); //从数据库是倒着排序搜索出来的，所以插到头部，后面就能够正确顺序显示
            }while (cursor.moveToNext());
        }
        cursor.close();
        return msgs;
    }

    public static void deleteDialogByThreadID(SQLiteDatabase appdb,String threadId){
        appdb.delete("dialogs","threadid=?",new String[]{threadId});
    }

    public static TDialog queryDialogByThreadID(SQLiteDatabase appdb,String threadId){
//        appdb=appdbHelper.getWritableDatabase();
        Cursor cursor=appdb.query("dialogs", null,"threadid=?",new String[]{threadId},null,null,null);
        if(cursor.moveToFirst()){
            int id=cursor.getInt(cursor.getColumnIndex("id"));
            String threadid=cursor.getString(cursor.getColumnIndex("threadid"));
            String threadname=cursor.getString(cursor.getColumnIndex("threadname"));
            String lastmsg=cursor.getString(cursor.getColumnIndex("lastmsg"));
            long lastmsgdate=cursor.getLong(cursor.getColumnIndex("lastmsgdate"));
            boolean isRead=cursor.getInt(cursor.getColumnIndex("isread"))==1;
            String imgpath=cursor.getString(cursor.getColumnIndex("imgpath"));
            boolean isSingle=cursor.getInt(cursor.getColumnIndex("issingle"))==1;
            boolean isVisible=cursor.getInt(cursor.getColumnIndex("isvisible"))==1;

            TDialog result=new TDialog(id, threadid, threadname, lastmsg,lastmsgdate, isRead, imgpath, isSingle, isVisible);
            cursor.close();
            return result;
        }else{
            cursor.close();
            return null;
        }
    }

    public static List<TDialog> queryAllDIalogs(SQLiteDatabase appdb){
        List<TDialog> dialogs=new LinkedList<>();
        Cursor cursor=appdb.query("dialogs",new String[]{"id","threadid","threadname","lastmsg","lastmsgdate","isread","imgpath","issingle"},
                "isvisible=?",new String[]{"1"},null,null,"lastmsgdate desc");
        if(cursor.moveToFirst()){
            do{
                int lookid=cursor.getInt(cursor.getColumnIndex("id"));
                String threadid=cursor.getString(cursor.getColumnIndex("threadid"));
                String threadname=cursor.getString(cursor.getColumnIndex("threadname"));
                String lastmsg=cursor.getString(cursor.getColumnIndex("lastmsg"));
                long lastmsgdate=cursor.getLong(cursor.getColumnIndex("lastmsgdate"));
                boolean isread=cursor.getInt(cursor.getColumnIndex("isread"))==1;
                String imgpath=cursor.getString(cursor.getColumnIndex("imgpath"));
                boolean isSingle=cursor.getInt(cursor.getColumnIndex("issingle"))==1;

                TDialog tDialog=new TDialog(lookid,threadid,threadname,lastmsg,lastmsgdate,isread,imgpath,isSingle,true);
                dialogs.add(tDialog);
            }while (cursor.moveToNext());
        }
        cursor.close();
        return dialogs;
    }

    //得到新消息时，对话要更新的内容可能有：图片、内容、时间
    public static TDialog dialogGetMsg(SQLiteDatabase appdb,TDialog tDialog,String threadId,String lastmsg,long lastmsgdate,String imgpath){
        ContentValues v=new ContentValues();
        v.put("lastmsg",lastmsg); tDialog.lastmsg=lastmsg;
        v.put("lastmsgdate",lastmsgdate); tDialog.lastmsgdate=lastmsgdate;
        v.put("imgpath",imgpath); tDialog.imgpath=imgpath;
        v.put("isread",0);
        appdb.update("dialogs",v,"threadid=?",new String[]{threadId});
        return tDialog;
    }


    public static boolean isMsgExist(SQLiteDatabase appdb, String blockid){
        Cursor cursor=appdb.query("msgs",null,"blockid=?",new String[]{blockid},null,null,null);
        if(cursor.moveToFirst()){
            cursor.close();
            return true;
        }else{
            cursor.close();
            return false;
        }
    }

    public static TDialog insertDialog(SQLiteDatabase appdb,String threadid, String threadname, String lastmsg, long lastmsgdate, int isRead, String imgpath, int isSingle, int isVisible){
        ContentValues v=new ContentValues();
        v.put("threadid",threadid);
        v.put("threadname",threadname);
        v.put("lastmsg",lastmsg);
        v.put("lastmsgdate",lastmsgdate);
        v.put("isread",isRead);
        v.put("imgpath",imgpath);
        v.put("issingle",isSingle);
        v.put("isvisible",isVisible);
        appdb.beginTransaction();
        appdb.insert("dialogs",null,v);
        appdb.setTransactionSuccessful();
        appdb.endTransaction();
        return new TDialog(1,threadid,threadname,lastmsg,lastmsgdate,isRead==1,imgpath,isSingle==1,isVisible==1);
    }

    public static TMsg insertMsg(SQLiteDatabase appdb,String threadid, int msgtype, String blockid, String authorname, String authoravatar,
                                 String body, long sendtime, int ismine){
        ContentValues v=new ContentValues();
        v.put("threadid",threadid);
        v.put("msgtype",msgtype);
        v.put("blockid",blockid);
        v.put("authorname",authorname);
        v.put("authoravatar",authoravatar);
        v.put("body",body);
        v.put("sendtime",sendtime);
        v.put("ismine",ismine);
        appdb.beginTransaction();
        appdb.insertOrThrow("msgs",null,v);
        appdb.setTransactionSuccessful();
        appdb.endTransaction();

        return new TMsg(blockid,threadid,msgtype,authorname,authoravatar,body,sendtime,ismine==1);
    }

    public static void changeDialogRead(SQLiteDatabase appdb,String threadid, int isread){
        ContentValues v=new ContentValues();
        v.put("isread",isread);
        appdb.beginTransaction();
        appdb.update("dialogs",v,"threadid=?",new String[]{threadid});
        appdb.setTransactionSuccessful();
        appdb.endTransaction();
        Log.d(TAG, "changeDialogRead: 将对话状态修改为已读");
    }

    public static List<String> getSyncPhotos(SQLiteDatabase appdb) {
        return null;
    }
}
