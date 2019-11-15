package sjtu.opennet.honvideo;

import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.text.DecimalFormat;

import wseemann.media.FFmpegMediaMetadataRetriever;
import sjtu.opennet.textilepb.Model.Video;

/**
 * This class extract the meta info from video file.
 * It can also generate HASH ID for a video.
 */
public class VideoMeta {
    private static final String TAG = "HONVIDEO.VideoMeta";
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    private MediaMetadataRetriever mdataReceiver;
    private FFmpegMediaMetadataRetriever fmdataReceiver;

    // Info contains
    // Info from input
    private String path;            // The Absolute path of local path.

    // Info extract from MediaMetadataRetriever
    private String width;           // Video width
    private String height;          // Video height
    private String duration;        // duration in mill second

    // Info extract from FmpegMediaMetadataRetriever
    private String filesize;        // File size in byte.
    private String creation;
    private String rotation;        // Video rotation
    private Bitmap thumbnail;

    // Info get from further process
    private String filename;        // File name read from file path.
    private String filesize_fmt;    // Formatted file size in KB/MB/GB/TB
    private long duration_long;     // duration with long type (used for computation)
    private String duration_fmt;    // formatted duration (hour:minute:second.mill)
    private byte[] thumbnail_byte;  // thumbnail in byte format
    private Bitmap thumbnail_small;         //small thumbnail with width 100px.
    private byte[] thumbnail_small_byte;    //small thumbnail in byte
    private String videoHash = "";

    /**
     * Constructor of VideoMeta.
     * All the Meta info is either extracted or computed within constructor.
     * @param filePath The absolute path of video. VideoMeta is video specified.
     */
    public VideoMeta(String filePath){
        path = filePath;
        mdataReceiver = new MediaMetadataRetriever();
        fmdataReceiver = new FFmpegMediaMetadataRetriever();

        try{
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
            thumbnail_byte = Bitmap2Bytes(thumbnail);
            thumbnail_small = getSmallThumbnail(100, thumbnail);
            thumbnail_small_byte = Bitmap2Bytes(thumbnail_small);
            creation = fmdataReceiver.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_CREATION_TIME);
            rotation = fmdataReceiver.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
            videoHash = getHashFromMeta();
        }catch(Exception e){
            e.printStackTrace();
        }finally{
            //Release the receiver no matter what.
            //Otherwise it may cause error when we use other receivers in future.
            mdataReceiver.release();
            fmdataReceiver.release();
        }
    }

    /**
     * List all the video info as a string.
     * It is a private method used by {@link #getHashFromMeta()} and {@link #logCatPrint()}<br />
     * <b>Note:<b/> stringInfo does not contain the video hash ID.
     * That is because the hash ID is exactly generated from stringInfo and thumbnail.
     *
     * @return A string shows all the meta info.
     */
    private String stringInfo(){
        return String.format("File Name: %s\n" +
                        "File Size: %s\n" +
                        "Frame Size: %s x %s\n" +
                        "Video Rotation: %s\n" +
                        "Video Duration: %s\n" +
                        "Video Creation: %s",
                filename, filesize_fmt, width, height, rotation, duration_fmt, creation);
    }

    /**
     * Output the meta info in logcat.
     */
    public void logCatPrint(){
        Log.i(TAG, stringInfo() + String.format("\nVideo Hash: %s", videoHash));
    }

    public static Bitmap getSmallThumbnail(int dstWidth, Bitmap sourceBitmap){
        if(sourceBitmap.getWidth()<=dstWidth){
            Log.e(TAG, "Source thumbnail is small enough.");
            return sourceBitmap;
        }
        int dstHeight = (int) (((double)dstWidth/(double)sourceBitmap.getWidth())*sourceBitmap.getHeight());
        Log.d(TAG, String.format("Get small thumbnail with %d x %d", dstWidth, dstHeight));
        return Bitmap.createScaledBitmap(sourceBitmap, dstWidth, dstHeight, false);
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
        //byte[] metaThumb = Bitmap2Bytes(thumbnail);
        byte[] meta = new byte[metaInfo.length + thumbnail_byte.length];
        System.arraycopy(metaInfo, 0, meta, 0, metaInfo.length);
        System.arraycopy(thumbnail_byte, 0, meta, metaInfo.length, thumbnail_byte.length);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(meta);

        //make sure to remove this before release

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



    public Video getPb(String posterHash){
        Video videopb = Video.newBuilder()
                .setId(videoHash)
                //.setCaption(stringInfo())
                .setCaption(filename)
                .setVideoLength((int)duration_long)
                .setPoster(posterHash)
                .build();

        //videopb.writeTo();
        return videopb;
    }

    public Video getPb2(String posterHash, String m3u8Content){
        Video videopb = Video.newBuilder()
                .setId(videoHash)
                //.setCaption(stringInfo())
                .setCaption(m3u8Content)
                .setVideoLength((int)duration_long)
                .setPoster(posterHash)
                .build();

        //videopb.writeTo();
        return videopb;
    }

    public String getHash(){
        return videoHash;
    }
    public byte[] getPosterByte(){
        return thumbnail_small_byte;
    }
    public Bitmap getPoster(){
        return thumbnail_small;
    }
}
