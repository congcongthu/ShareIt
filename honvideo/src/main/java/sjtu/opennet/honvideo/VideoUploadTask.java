package sjtu.opennet.honvideo;


import android.media.MediaMetadataRetriever;
import android.util.Log;
import java.nio.file.Files;
import java.nio.file.Paths;

import sjtu.opennet.hon.Handlers;
import sjtu.opennet.hon.Textile;
import sjtu.opennet.textilepb.Model.VideoChunk;

/**
 * Video upload task.
 *
 */

public class VideoUploadTask {
    //Variables get from constructor
    private String videoId;
    private String tsPath;
    private String tsAbsolutePath;
    private boolean endTag;
    private final String TAG = "HONVIDEO.VideoTask";

    //Variables assigned during running
    private int currentDuration = 0;
    private int duration_int = 0;

    public VideoUploadTask(String videoId, String tsPath, String tsAbsolutePathPath, boolean endTag){
        this.videoId = videoId;
        this.tsPath = tsPath;
        this.tsAbsolutePath = tsAbsolutePathPath;
        this.endTag = endTag;
    }

    /**
     * This handler is called by ipfsAddData
     * It does the following things:<br />
     *  - create VideoChunk proto object<br />
     *  - add it to thread<br />
     *  - upload it to cafe peer
     *
     * @TODO:
     * Use two blocking queue to do segment and upload.
     */
    private Handlers.IpfsAddDataHandler tsHandler = new Handlers.IpfsAddDataHandler() {
        @Override
        public void onComplete(String path) {
            Log.d(TAG, String.format("ts chunk ipfs path: %s", path));
            VideoChunk videoChunk = VideoChunk.newBuilder()
                    .setId(videoId)
                    .setChunk(tsPath)
                    .setAddress(path)
                    .setStartTime(currentDuration)
                    .setEndTime(currentDuration + duration_int)
                    .build();
            try {
                Textile.instance().videos.addVideoChunk(videoChunk);
                Textile.instance().videos.publishVideoChunk(videoChunk);
            }catch(Exception e){
                Log.e(TAG, "Unexpected error when publish video chunk");
                e.printStackTrace();
            }
        }

        @Override
        public void onError(Exception e) {
            Log.e(TAG, String.format("ts upload failed"));
            e.printStackTrace();
        }
    };

    public static VideoUploadTask endTask(){
        return new VideoUploadTask("","", "", true);
    }



    /**
     * upload does following things:
     *  - Read duration info
     *  - Upload to IPFS
     *  - Create Video Chunk object
     *  - Upload to Thread
     *  - Publish to Cafe.
     * @param currentDuration The duration now. Used as startTime;
     * @return endTime. Used to update duration in video Uploader.
     */
    public int upload(int currentDuration){
        this.currentDuration = currentDuration;
        if(endTag){
            Log.d(TAG, "End task received. Return -1 to end the task thread.");
            return -1;
        }
        Log.d(TAG, String.format("Video Upload Task Begin, Chunk Start Duration %d", currentDuration));
        //int duration_int = 0;
        MediaMetadataRetriever mdataReceiver = null;
        try {
            mdataReceiver = new MediaMetadataRetriever();
            mdataReceiver.setDataSource(tsAbsolutePath);
            String duration = mdataReceiver.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);

            byte[] fileContent = Files.readAllBytes(Paths.get(tsAbsolutePath));
            if (duration == null) {
                Log.w(TAG, "Can not extract dutation.");
                duration_int = -1;
                return -1;
            } else {
                duration_int = Integer.parseInt(duration);
                Textile.instance().ipfs.ipfsAddData(fileContent, true, false, tsHandler);
            }
            Log.d(TAG, String.format("Video Upload Task Done, Chunck End Duration: %d", currentDuration + duration_int));

        }catch(Exception e){
            e.printStackTrace();
        }finally{
            //Important: Always release the retriever
            if(mdataReceiver != null){
                mdataReceiver.release();
            }
        }
        return currentDuration + duration_int;
    }
}
