package sjtu.opennet.honvideo;

import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.util.Log;

//import com.github.hiteshsondhi88.libffmpeg.FFmpeg;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;

import wseemann.media.FFmpegMediaMetadataRetriever;
import sjtu.opennet.textilepb.Model.Video;
import static java.lang.Long.getLong;

public class VideoMeta {
    private static final String TAG = "VideoMeta";
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    private MediaMetadataRetriever mdataReceiver;
    private FFmpegMediaMetadataRetriever fmdataReceiver;

    //private String title;
    private String path;
    private String filename;
    private String filesize;
    private String filesize_fmt;
    private String width;
    private String height;
    private String rotation;
    private String duration;        //duration in mill second
    private long duration_long;     //duration with long type (used for computation)
    private String duration_fmt;    //formatted duration (hour:minute:second.mill)
    //private String bitrate;         //the average bitrate (in bits/sec)
    //private String bitrate_fmt;     //formatted bitrate (KB/S)
    private String creation;
    private Bitmap thumbnail;
    private String videoHash = "";

    //private String date;            //the date when the data source was created or modified
    //private String genre;           //the content type or genre of the data source.
    private String stringInfo(){
        return String.format("File Name: %s\n" +
                        "File Size: %s\n" +
                        "Frame Size: %s x %s\n" +
                        "Video Rotation: %s\n" +
                        "Video Duration: %s\n" +
                        "Video Creation: %s",
                filename, filesize_fmt, width, height, rotation, duration_fmt, creation);
    }

    public void logCatPrint(){
        Log.i(TAG, stringInfo() + String.format("\nVideo Hash: %s", videoHash));
    }
    public VideoMeta(String filePath){
        path = filePath;

        mdataReceiver = new MediaMetadataRetriever();
        fmdataReceiver = new FFmpegMediaMetadataRetriever();

        mdataReceiver.setDataSource(filePath);
        fmdataReceiver.setDataSource(filePath);
        String[] pathSep = filePath.split(File.separator);
        filename = pathSep[pathSep.length-1];
        filesize = fmdataReceiver.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_FILESIZE);
        if(filesize == null){
            Log.w(TAG, "Can not extract file size.");
        }
        else {
            filesize_fmt = formatFilesize(filesize);
        }
        //title = mdataReceiver.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
        width = mdataReceiver.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
        height = mdataReceiver.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
        duration = mdataReceiver.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        if(duration == null){
            Log.w(TAG, "Can not extract dutation.");
            thumbnail = extractFrameFFmpeg(1);
        }
        else {
            duration_long = Long.parseLong(duration);
            duration_fmt = formatDuration(duration_long);

            if (duration_long < 60000) {
                thumbnail = extractFrameFFmpeg(1);
            } else {
                thumbnail = extractFrameFFmpeg(duration_long / 10);
            }
        }
        //bitrate = fmdataReceiver.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_VARIANT_BITRATE);
        //bitrate_fmt = formatBitrate(bitrate);
        //date = mdataReceiver.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE);
        creation = fmdataReceiver.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_CREATION_TIME);
        //genre = mdataReceiver.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE);
        rotation = fmdataReceiver.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);

        try{
            videoHash = getHashFromMeta();
        }catch(Exception e){
            e.printStackTrace();
        }

        mdataReceiver.release();
        fmdataReceiver.release();
    }


    /**
     * Create video meta.
     * @param filePath
     * @param hashvideo Whether to hash the whole video.
     *                  true: hash the whole video. Around 5 sec/GB.
     *                  false: hash meta only. Really fast.
     */
    public VideoMeta(String filePath, boolean hashvideo){
        this(filePath);

        if(hashvideo) {
            try {
                videoHash = getHashFromVideo();
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    private String formatDuration(long duration){

        long min, sec, hour;
        hour = duration / 3600000;
        duration -= hour * 3600000;
        min = duration / 60000;
        duration -= min * 60000;
        sec = duration / 1000;
        duration -= sec * 1000;
        return String.format("%d:%02d:%02d.%03d", hour, min, sec, duration);
    }

    private String formatFilesize(String sizeByte){
        long sizeLong = Long.parseLong(sizeByte);
        final String[] units = new String[] { "B", "kB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(sizeLong)/Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(sizeLong/Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    private String formatBitrate(String rateBits){
        float rateFloat = Float.parseFloat(rateBits);
        return String.format("%s KB/S", String.valueOf(rateFloat/1024));
    }

    private Bitmap extractFrame(long timeMs){
        Bitmap bitmap = null;
        // Try to get 1 sec further if get frame failed.
        for(long i = timeMs; i<duration_long; i += 1000){
            bitmap = mdataReceiver.getFrameAtTime(i*1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
            if(bitmap != null){
                break;
            }
            Log.w(TAG, "Get frame fail. try to get one sec further");
        }
        return bitmap;
    }

    private Bitmap extractFrameFFmpeg(long timeMs){
        Bitmap bitmap = null;
        // Try to get 1 sec further if get frame failed.
        for(long i = timeMs; i<duration_long; i += 1000){
            bitmap = fmdataReceiver.getFrameAtTime(i*1000, FFmpegMediaMetadataRetriever.OPTION_CLOSEST_SYNC);
            if(bitmap != null){
                break;
            }
            Log.w(TAG, "Get frame fail. try to get one sec further");
        }
        return bitmap;
    }


    public String saveThumbnail(String dirPath){
        try {
            File img = new File(dirPath, "thumbnail.png");
            OutputStream fout = new FileOutputStream(img);
            thumbnail.compress(Bitmap.CompressFormat.PNG, 100, fout);
            fout.flush();
            fout.close();
            return img.getAbsolutePath();
        }catch(FileNotFoundException fe){
            Log.e(TAG, "File not found");
            fe.printStackTrace();
        }catch(IOException ie){
            Log.e(TAG, "ioerror");
            ie.printStackTrace();
        }
        return "";
    }

    public String getHashFromVideo() throws Exception{
        byte[] buffer = new byte[10485760]; // 10MB
        int count;

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(path));
        while((count = bis.read(buffer))>0){
            digest.update(buffer, 0, count);
        }
        byte[] hash = digest.digest();
        return java.util.Base64.getEncoder().encodeToString(hash);

    }

    public String getHashFromMeta() throws Exception{
        //Log.d(TAG, stringInfo());

        byte[] metaInfo = stringInfo().getBytes();
        byte[] metaThumb = Bitmap2Bytes(thumbnail);
        byte[] meta = new byte[metaInfo.length + metaThumb.length];
        System.arraycopy(metaInfo, 0, meta, 0, metaInfo.length);
        System.arraycopy(metaThumb, 0, meta, metaInfo.length, metaThumb.length);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(meta);

        // make sure to remove this before release
        /*
        Log.d(TAG, String.format("metaInfo:\n%s\nmetaThumb:\n%s", new String(metaInfo), new String(metaThumb), new String(meta)));
        */
        //return java.util.Base64.getEncoder().encodeToString(hash);
        return bytesToHex(hash);
    }



    private byte[] Bitmap2Bytes(Bitmap bp){
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bp.compress(Bitmap.CompressFormat.PNG, 100, bos);
        return bos.toByteArray();
    }

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    /**
     * Test the performance for FFmpegMediaMetadataRetriever
     * DO NOT FORGET TO REMOVE THIS FUNCTION BEFORE RELEASE
     * @param filePath
     * @param tool
     */
    public VideoMeta(String filePath, String tool){
        Log.i(TAG, "Compare the performance of MediaMetadataRetriever and FFmpegMediaMetadataRetriever");
        Log.i(TAG, "MediaMetadataRetriever:");

        long startTime = System.currentTimeMillis();
        path = filePath;

        mdataReceiver = new MediaMetadataRetriever();
        mdataReceiver.setDataSource(filePath);

        //title = mdataReceiver.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
        width = mdataReceiver.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
        height = mdataReceiver.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
        duration = mdataReceiver.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        duration_long = Long.parseLong(duration);
        duration_fmt = formatDuration(duration_long);

        if(duration_long < 60000){
            thumbnail = extractFrame(0);
        }
        else{
            thumbnail = extractFrame(duration_long / 100);
        }
        //bitrate = mdataReceiver.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
        //date = mdataReceiver.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE);
        //genre = mdataReceiver.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE);

        long endTime = System.currentTimeMillis();
        Log.i(TAG, String.format("%d ms", endTime - startTime));
        logCatPrint();

        Log.i(TAG, "FFmpegMediaMetadataRetriever:");
        startTime = System.currentTimeMillis();
        FFmpegMediaMetadataRetriever mmr = new FFmpegMediaMetadataRetriever();
        mmr.setDataSource(filePath);
        String ftitle = mmr.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_TITLE);
        String fduration = mmr.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_DURATION);
        long fduration_long = Long.parseLong(fduration);
        String fduration_fmt = formatDuration(fduration_long);
        String fbitrate = mmr.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_VARIANT_BITRATE);
        String fdate = mmr.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_DATE);
        String fcreation = mmr.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_CREATION_TIME);
        String fgenre = mmr.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_GENRE);
        String ffilesize = mmr.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_FILESIZE);

        Bitmap fb = mmr.getFrameAtTime(2000000, FFmpegMediaMetadataRetriever.OPTION_CLOSEST_SYNC);
        byte[] artwork = mmr.getEmbeddedPicture();
        mmr.release();
        endTime = System.currentTimeMillis();

        Log.i(TAG, String.format("%d ms", endTime - startTime));
        Log.i(TAG, String.format("Video meta:\n" +
                        "Video Title: %s\n" +
                        "Video Duration: %s\n" +
                        "Video Bitrate: %s\n" +
                        "Video Date: %s\n" +
                        "Video Creation: %s\n" +
                        "Video Genre: %s\n" +
                        "File Size: %s\n",
                ftitle, fduration_fmt, fbitrate, fdate, fcreation, fgenre, ffilesize));
        //String ftitle =
    }

    public Video getPb(){
        Video videopb = Video.newBuilder()
                .setId(videoHash)
                .setCaption(stringInfo())
                .setVideoLength((int)duration_long)
                .build();

        return videopb;
    }

    public String getHash(){
        return videoHash;
    }
}
