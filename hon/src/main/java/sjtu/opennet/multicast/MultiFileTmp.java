package sjtu.opennet.multicast;

import java.util.PriorityQueue;

public class MultiFileTmp {
    public String fileId;
//    public String threadId;
//    public String senderAddr;
//    public String fileName;
//    public Timestamp sendTime;

    public PriorityQueue<IndexPacket> datas=new PriorityQueue<>();

//    public MultiFileTmp(String fileId, String threadId, String senderAddr, String fileName, Timestamp timestamp) {
//        this.fileId = fileId;
//        this.threadId = threadId;
//        this.senderAddr = senderAddr;
//        this.fileName = fileName;
//        this.sendTime=timestamp;
//    }


    public MultiFileTmp(String fileId) {
        this.fileId = fileId;
    }

    public static class IndexPacket implements Comparable<IndexPacket>{
        public long index;
        public byte[] data;

        @Override
        public int compareTo(IndexPacket indexPacket) {
            if(this.index > indexPacket.index){
                return 1;
            }else if(this.index < indexPacket.index){
                return -1;
            }else{
                return 0;
            }
        }
    }
}
