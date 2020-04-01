package sjtu.opennet.stream.video.ticketvideo;

import android.util.Log;

public class VideoGetter_tkt {
    private static final String TAG = "HONVIDEO.VideoGetter_tkt";

    String videoId;

    public VideoGetter_tkt(String videoId) {
        this.videoId = videoId;

        Log.d(TAG, "VideoGetter_tkt: get the video: "+videoId);
    }

    public void startGet(){

    }

    public void stopGet(){

    }
}
