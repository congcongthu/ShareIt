package sjtu.opennet.stream.video;

import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;

import sjtu.opennet.stream.util.FileUtil;

public class M3U8Util {
    private static final String TAG = "============HONVIDEO.M3U8Util";
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

    public synchronized static ArrayList<ChunkInfo> getInfos(String listPath){
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

    private synchronized static ChunkInfo parseM3u8Content(String infoLine, String filename){
        long duration = (long)(Double.parseDouble(infoLine.substring(8, infoLine.length()-1))*1000000);
        return new ChunkInfo(filename, duration);
    }

    public synchronized static File initM3u8(String dir, String videoId){
        String head="#EXTM3U\n" +
                "#EXT-X-VERSION:3\n" +
                "#EXT-X-MEDIA-SEQUENCE:0\n" +
                "#EXT-X-ALLOW-CACHE:YES\n" +
                "#EXT-X-TARGETDURATION:5\n"; // +
//                "#EXT-X-PLAYLIST-TYPE:EVENT\n";
        File m3u8file=new File(dir+"/"+videoId+".m3u8");

        try{
            FileWriter fileWriter=new FileWriter(m3u8file,true);
            fileWriter.write(head);
            fileWriter.flush();
            fileWriter.close();
        }catch (Exception e){
            e.printStackTrace();
        }

        return m3u8file;
    }

    public synchronized static void writeM3u8(File m3u8file, long end,long start, String chunkName){
        long duration0=end-start; //微秒
        double size = (double)duration0/1000000;
        DecimalFormat df = new DecimalFormat("0.000000");//格式化小数，不足的补0
        String duration = df.format(size);//返回的是String类型的
        String append = "#EXTINF:"+duration+",\n"+
                chunkName+"\n";
        try {
            FileWriter fileWriter=new FileWriter(m3u8file,true);
            fileWriter.write(append);
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized static void writeM3u8End(File m3u8file){
        try {
            FileWriter fileWriter=new FileWriter(m3u8file,true);
            fileWriter.write("#EXT-X-ENDLIST");
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean existOrNot(String tspath){
        long tsLength=(new File(tspath)).list().length-1;
        Log.d(TAG, "existOrNot: "+tsLength);
        if(tsLength>0){
            return true;
        }
        return false;
    }
}
