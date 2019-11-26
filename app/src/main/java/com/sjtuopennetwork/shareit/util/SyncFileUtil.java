package com.sjtuopennetwork.shareit.util;

import android.util.Log;

import com.google.protobuf.Timestamp;
//import com.google.protobuf.util;
import com.google.protobuf.util.Timestamps;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;

import sjtu.opennet.hon.Handlers;
import sjtu.opennet.hon.Textile;
import sjtu.opennet.textilepb.Model.SyncFile;
import sjtu.opennet.textilepb.QueryOuterClass;

public class SyncFileUtil {
    private static final String TAG = "SyncFileUtil";
    String filePath;
    String fileHash;
    String peerAddress;
    SyncFile.Type sType;
    final Object LOCK = new Object();

    private Handlers.IpfsAddDataHandler ipfsHandler = new Handlers.IpfsAddDataHandler() {
        @Override
        public void onComplete(String path) {
            synchronized (LOCK) {
                Log.d(TAG, String.format("File ipfs path: %s", path));
                fileHash = path;
                LOCK.notify();
            }
        }

        @Override
        public void onError(Exception e) {
            synchronized (LOCK) {
                Log.e(TAG, String.format("Unexpect ipfs error when add file %s", filePath));
                e.printStackTrace();
                LOCK.notify();
            }
        }
    };

    public SyncFileUtil(String filePath, String peerAddress, SyncFile.Type sType){
        this.filePath = filePath;
        this.peerAddress = peerAddress;
        this.sType = sType;
    }

    public void Add(){
        try {
            byte[] fileContent = Files.readAllBytes(Paths.get(filePath));
            synchronized (LOCK) {
                Textile.instance().ipfs.ipfsAddData(fileContent, true, false, ipfsHandler);
                Log.d(TAG, "Wait for ipfs complete");
                LOCK.wait();
                Log.d(TAG, "Lock notified");
            }
        }catch(InterruptedException ie){
            ie.printStackTrace();
        }catch(IOException ie){
            ie.printStackTrace();
        }
        executor(SyncFile.Operation.ADD);
    }


    public void Sync(){

    }

    private static Timestamp getTimeStamp(){
        return Timestamps.fromMillis(new Date().getTime());
    }

    /**
     * @TODO Add file name to SyncFile
     * @param op
     */
    private void executor(SyncFile.Operation op){
        SyncFile sFile =  SyncFile.newBuilder()
                .setPeerAddress(peerAddress)
                .setFile(fileHash)
                .setType(sType)
                .setDate(getTimeStamp())
                .setOperation(op)
                .build();
        try {
            Textile.instance().files.addSyncFile(sFile);
            Textile.instance().files.publicSuynFile(sFile);
        }catch(Exception e){
            Log.e(TAG, "Error occur when publish syncfile");
            e.printStackTrace();
        }
    }

    public static void searchSyncFiles(String peerAddress, SyncFile.Type sType){
        QueryOuterClass.QueryOptions options = QueryOuterClass.QueryOptions.newBuilder()
                .setWait(1)
                .setLimit(1)
                .build();
        QueryOuterClass.SyncFileQuery query=QueryOuterClass.SyncFileQuery.newBuilder()
                .setAddress(peerAddress)
                .setType(sType)
                .build();
        try {
            Textile.instance().files.searchSyncFiles(query, options);
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
