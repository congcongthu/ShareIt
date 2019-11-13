package sjtu.opennet.honvideo;

import java.text.DecimalFormat;

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

}
