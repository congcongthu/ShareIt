package sjtu.opennet.honvideo;

import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.util.Log;

import static java.lang.Long.getLong;

public class VideoMeta {
    private static final String TAG = "VideoMeta";
    private MediaMetadataRetriever mdataReceiver;

    public String width;
    public String height;
    public String duration;
    public long duration_long;
    public String duration_fmt;
    public Bitmap thumbnail;


    public VideoMeta(String filePath){
        mdataReceiver = new MediaMetadataRetriever();
        mdataReceiver.setDataSource(filePath);
        width = mdataReceiver.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
        height = mdataReceiver.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
        duration = mdataReceiver.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        duration_long = Long.parseLong(duration);
        duration_fmt = formatDuration(duration_long);
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

    private Bitmap extractFrame(long timeMs){
        Bitmap bitmap = null;
        // Try to get 1 sec further if get frame failed.
        for(long i = timeMs; i<duration_long; i += 1000){
            bitmap = mdataReceiver.getFrameAtTime(i*1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
            if(bitmap != null){
                break;
            }
        }
        return bitmap;
    }

    public void logCatPrint(){
        Log.i(TAG, String.format("Meta info:\n" +
                "Video Size: %s x %s\n" +
                "Video Duration %s",
                width, height, duration_fmt));
    }

}
