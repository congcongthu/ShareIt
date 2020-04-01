package sjtu.opennet.stream.video;

import java.util.ArrayList;

import sjtu.opennet.stream.util.FileUtil;

public class M3U8Util {

    public static class ChunkInfo{
        public String filename;
        public long duration;
        ChunkInfo(String filename, long duration){
            this.filename = filename;
            this.duration = duration;
        }
    }

    public static String getHead(int targetDuration, boolean addNewLine){
        String head=String.format("#EXTM3U\n" +
                "#EXT-X-VERSION:3\n" +
                "#EXT-X-MEDIA-SEQUENCE:0\n" +
                "#EXT-X-ALLOW-CACHE:YES\n" +
                "#EXT-X-TARGETDURATION:%d\n" +
                "#EXT-X-PLAYLIST-TYPE:EVENT", targetDuration);
        if(addNewLine){
            return head + "\n";
        }else{
            return head;
        }
    }

    public static ArrayList<ChunkInfo> getInfos(String listPath){
        ArrayList<ChunkInfo> infos = new ArrayList<>();
        String m3u8content = FileUtil.readAllString(listPath);
        String[] list = m3u8content.split("\n");
        int listLen = list.length;
        for (int i=5; i< listLen - 1 ; i ++){
            if(i % 2 != 0){
                ChunkInfo info = parseM3u8Content(list[i], list[i+1]);
                infos.add(info);
            }
        }
        return infos;
    }

    private static ChunkInfo parseM3u8Content(String infoLine, String filename){
        long duration = (long)(Double.parseDouble(infoLine.substring(8, infoLine.length()-1))*1000000);
        return new ChunkInfo(filename, duration);
    }
}
