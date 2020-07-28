package sjtu.opennet.multicast;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.util.Log;

import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Arrays;
import java.util.HashMap;

import sjtu.opennet.hon.Handlers;
import sjtu.opennet.hon.MulticastFile;
import sjtu.opennet.multiproto.Multicast;
import sjtu.opennet.stream.util.FileUtil;

public class FileMultiCaster {
    private static final String TAG = "================FileMultiCaster";

    static WifiManager.MulticastLock multicastLock;
    static MulticastSocket socket;
    static InetAddress multiAddress;
    static int MULTI_PORT = 18611;
    static String MULTI_ADDR = "239.0.0.3";
    static String myAddress;

    static HashMap<String,MultiFileTmp> multiFileMap=new HashMap();
    private static String multiFileDir= Environment.getExternalStorageDirectory().getAbsolutePath() + "/txtlmulticast/";

    public static void startListen(Context context, Handlers.MultiFileHandler multiFileHandler){
        File file=new File(multiFileDir);
        if(!file.exists()){
            file.mkdir();
        }

        //打开组播
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        multicastLock = wifiManager.createMulticastLock("multicast.test");
        multicastLock.acquire();
        System.out.println("get multicast lock");
        try {
            socket = new MulticastSocket(MULTI_PORT);
            multiAddress = InetAddress.getByName(MULTI_ADDR);
            socket.joinGroup(multiAddress);
            socket.setLoopbackMode(true);
        } catch (Exception e) {
            e.printStackTrace();
        }


        new Thread(){
            @Override
            public void run() {
                while(true){
                    byte[] buf=new byte[65530];
                    DatagramPacket packet=new DatagramPacket(buf,buf.length);
                    try{
                        socket.receive(packet);
                        int plength=packet.getLength();
                        byte[] pByte= Arrays.copyOf(buf,plength);
                        Multicast.packet multiPacket=Multicast.packet.parseFrom(pByte);
                        Log.d(TAG, "run: get data: "+plength);
                        if(multiPacket.getPacketType()==3){
                            Log.d(TAG, "run: get heart beat");
                            continue;
                        }
                        switch(multiPacket.getPacketType()){
                            case 0:
                                Log.d(TAG, "run: get meta: "+multiPacket.getFileName());
                                MultiFileTmp multiFileTmp=new MultiFileTmp(multiPacket.getFileId());
                                multiFileMap.put(multiPacket.getFileId(),multiFileTmp);
                                break;
                            case 1:
                                Log.d(TAG, "run: get data: "+multiPacket.getFileId()+" "+multiPacket.getIndex());
                                MultiFileTmp multiFileTmp1=multiFileMap.get(multiPacket.getFileId());
                                if(multiFileTmp1!=null){
                                    MultiFileTmp.IndexPacket indexPacket = new MultiFileTmp.IndexPacket();
                                    indexPacket.index=multiPacket.getIndex();
                                    indexPacket.data=multiPacket.getData().toByteArray();
                                    multiFileTmp1.datas.add(indexPacket);
                                }else{
                                    Log.d(TAG, "run: no fileid: "+multiPacket.getFileId());
                                }
                                break;
                            case 2:
                                //开始组装和存储
                                MultiFileTmp multiFileTmp2=multiFileMap.get(multiPacket.getFileId());
                                String filePathName=multiFileDir+"/"+ multiPacket.getFileName();
                                BufferedOutputStream bfo=new BufferedOutputStream(new FileOutputStream(filePathName));
                                if(multiFileTmp2!=null){
                                    int multiSize=multiFileTmp2.datas.size();
                                    Log.d(TAG, "run: get end: byte[] nums: "+multiSize);
                                    for(int i=0;i<multiSize;i++){
                                        byte[] tmp=multiFileTmp2.datas.poll().data;
                                        bfo.write(tmp);
                                        bfo.flush();
                                    }
                                }
                                bfo.close();

                                MulticastFile multicastFile=new MulticastFile(
                                        multiPacket.getThreadId(),
                                        multiPacket.getFileId(),
                                        multiPacket.getSender(),
                                        multiPacket.getFileName(),
                                        filePathName,
                                        multiPacket.getSendTime().getSeconds());

                                multiFileHandler.onGetMulticastFile(multicastFile);
                                break;
                        }

                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
        }.start();

        new Thread(){
            @Override
            public void run() {
                while(true) {
                    try {
                        Thread.sleep(30000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    //发送心跳
                    Multicast.packet packetEnd = Multicast.packet.newBuilder()
                            .setPacketType(3) // meta类型
                            .build();
                    byte[] endByte = packetEnd.toByteArray();
                    DatagramPacket endPacket = new DatagramPacket(
                            endByte,
                            endByte.length,
                            multiAddress,
                            MULTI_PORT
                    );
                    sendPacket(endPacket);
                    Log.d(TAG, "run: send heart beat: " + endByte.length);

                }
            }
        }.start();

    }

    public static void sendMulticastFile(int sleepTime,MulticastFile multicastFile){
        myAddress=multicastFile.getSenderAddress();
        new Thread() {
            @Override
            public void run() {
                String filePath=multicastFile.getFilePath();
                Log.d(TAG, "run: try to send multicast : "+ filePath+" sleepTime: "+sleepTime);

                try {
                    socket.joinGroup(multiAddress);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    //发送源meta
                    long nowTime=System.currentTimeMillis();
                    String fileId=String.valueOf(nowTime);
                    Multicast.packet packet= Multicast.packet.newBuilder()
                            .setSender(myAddress)
                            .setPacketType(0) // meta类型
                            .setFileName(multicastFile.getFileName())
                            .setThreadId(multicastFile.getThreadId())
                            .setFileId(fileId)
                            .build();
                    byte[] meta=packet.toByteArray();
                    DatagramPacket datagramPacket = new DatagramPacket(
                            meta,
                            meta.length,
                            multiAddress,
                            MULTI_PORT
                    );
                    sendPacket(datagramPacket);
                    Log.d(TAG, "run: send meta: "+meta.length);
                    Thread.sleep(100);

                    //发送数据
                    BufferedInputStream bfi=new BufferedInputStream(new FileInputStream(filePath));
                    byte[] tmpData=new byte[65000];
                    int readNum=0;
                    int index=1;
                    while((readNum=bfi.read(tmpData))!=-1){
                        Multicast.packet dataPacket=Multicast.packet.newBuilder()
                                .setIndex(index)
                                .setSender(myAddress)
                                .setPacketType(1) //数据packet
                                .setFileId(fileId)
                                .setData(ByteString.copyFrom(tmpData,0,readNum))
                                .build();
                        byte[] datas=dataPacket.toByteArray();
                        DatagramPacket tmpPacket = new DatagramPacket(
                                datas,
                                datas.length,
                                multiAddress,
                                MULTI_PORT
                        );
                        sendPacket(tmpPacket);
                        Thread.sleep(sleepTime);
                        Log.d(TAG, "run: send data: "+index+ " " +datas.length);
                        index++;
                    }
                    bfi.close();

                    //发送结束
                    Multicast.packet packetEnd= Multicast.packet.newBuilder()
                            .setSender(myAddress)
                            .setPacketType(2) // meta类型
                            .setFileId(fileId)
                            .setFileName(multicastFile.getFileName())
                            .setThreadId(multicastFile.getThreadId())
                            .setFileId(fileId)
                            .setSendTime(Timestamp.newBuilder().setSeconds(nowTime/1000).build())
                            .build();
                    byte[] endByte=packetEnd.toByteArray();
                    DatagramPacket endPacket = new DatagramPacket(
                            endByte,
                            endByte.length,
                            multiAddress,
                            MULTI_PORT
                    );
                    sendPacket(endPacket);
                    Log.d(TAG, "run: send end: "+endByte.length);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private static void sendPacket(DatagramPacket datagramPacket){
        try {
            socket.send(datagramPacket);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
