package sjtu.opennet.multicast;

import java.util.PriorityQueue;

public class MultiFileTmpData {
    public String fileId;
    public PriorityQueue<IndexPacket> datas=new PriorityQueue<>();

    public MultiFileTmpData(String fileId) {
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
