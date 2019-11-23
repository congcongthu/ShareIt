package com.sjtuopennetwork.shareit.util;

import android.util.Log;

import com.google.protobuf.Timestamp;
//import com.google.protobuf.util;
import com.google.protobuf.util.Timestamps;


import java.util.Date;

import sjtu.opennet.hon.Handlers;
import sjtu.opennet.textilepb.Model.SyncFile;

public class SyncFileUtil {
    String filePath;
    String fileHash;
    String peerAddress;
    SyncFile.Type sType;

//    private Handlers.IpfsAddDataHandler posterHandler = new Handlers.IpfsAddDataHandler() {
//        @Override
//        public void onComplete(String path) {
//            synchronized (POSTERLOCK) {
//                Log.d(TAG, String.format("Poster ipfs path: %s", path));
//                posterHash = path;
//                POSTERLOCK.notify();
//            }
//        }
//
//        @Override
//        public void onError(Exception e) {
//            e.printStackTrace();
//            POSTERLOCK.notify();
//        }
//    };

    public SyncFileUtil(String filePath, String peerAddress, SyncFile.Type sType){
        this.filePath = filePath;
        this.peerAddress = peerAddress;
        this.sType = sType;
    }

    public void syncAdd(){

    }

    private static Timestamp getTimeStamp(){

        return Timestamps.fromMillis(new Date().getTime());
    }

    private void executor(SyncFile.Operation op){
        SyncFile sFile =  SyncFile.newBuilder()
                .setPeerAddress(peerAddress)
                .setFile(fileHash)
                .setType(sType)
                .setDate(getTimeStamp())
                .setOperation(op)
                .build();
    }
}
