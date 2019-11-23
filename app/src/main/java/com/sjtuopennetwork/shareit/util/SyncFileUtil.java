package com.sjtuopennetwork.shareit.util;

import com.google.protobuf.Timestamp;
//import com.google.protobuf.util;
import com.google.protobuf.util.Timestamps;


import java.util.Date;

import sjtu.opennet.textilepb.Model.SyncFile;

public class SyncFileUtil {
    String filePath;
    String fileHash;
    String peerAddress;
    SyncFile.Type sType;
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
