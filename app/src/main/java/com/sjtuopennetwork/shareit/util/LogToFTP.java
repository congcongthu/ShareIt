package com.sjtuopennetwork.shareit.util;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import java.io.FileInputStream;
import java.io.IOException;

public class LogToFTP {

    static FTPClient client =new FTPClient();

    private static final String host="";
    private static final int port=0;
    private static final String uname="";
    private static final String password="";

    public static boolean uploadLogToFTP(String filePath){
        //建立连接
        try {
            client.connect(host,port);
            if(FTPReply.isPositiveCompletion(client.getReplyCode())){
                boolean status=client.login(uname,password);
                client.enterLocalPassiveMode(); // ?
            }

            FileInputStream inputStream=new FileInputStream(filePath);
            client.storeFile("",inputStream);
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

}
