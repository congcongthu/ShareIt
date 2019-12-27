package sjtu.opennet.honvideo;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class m3u8Helper {



    private static DecimalFormat df6 =  new DecimalFormat("0.000000");

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

    public static String getTs(String prefix, int index, int durationMs, boolean addNewLine){
        DecimalFormat df = new DecimalFormat("");

        //String duration_fmt = (float) durationMs/ 1000;
        return "";
    }


    static class chunkInfo{
        public String filename;
        public long duration;
        chunkInfo(String filename, long duration){
            this.filename = filename;
            this.duration = duration;
        }
    }

    private static chunkInfo parseM3u8Content(String infoLine, String filename){


        //Log.d(TAG, String.format("Line: %s", infoLine));
        //Log.d(TAG, String.format("Line: %s", infoLine.substring(8, infoLine.length()-1)));
        long duration = (long)(Double.parseDouble(infoLine.substring(8, infoLine.length()-1))*1000000);
        return new chunkInfo(filename, duration);
    }

    public static ArrayList<chunkInfo> getInfos(String listPath){
        ArrayList<chunkInfo> infos = new ArrayList<>();
        String m3u8content = FileUtil.readAllString(listPath);
        String[] list = m3u8content.split("\n");
        int listLen = list.length;
        for (int i=5; i< listLen - 1 ; i ++){
            if(i % 2 != 0){
                chunkInfo info = parseM3u8Content(list[i], list[i+1]);
                infos.add(info);
            }
        }
        return infos;
    }
}
