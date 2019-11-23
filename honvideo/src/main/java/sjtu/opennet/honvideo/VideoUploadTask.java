package sjtu.opennet.honvideo;


import android.media.MediaMetadataRetriever;
import android.util.Log;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.BlockingQueue;

import sjtu.opennet.hon.Handlers;
import sjtu.opennet.hon.Textile;
import sjtu.opennet.textilepb.Model.VideoChunk;

/**
 * Video upload task.
 * It does the following task:<br />
 *  -
 */

public class VideoUploadTask {
    private static final String TAG = "HONVIDEO.VideoUploadTask";
    private static final TimeLog timeLog = new TimeLog("HONVIDEO.VideoUploadTask");

    //private boolean ipfsComplete = false;

    //lock object used to let main thread wait for ipfs thread.
    final Object LOCK = new Object();

    //Variables get from constructor
    private String videoId;
    private String tsPath;
    private String tsAbsolutePath;
    private boolean endTag;

    //Variables assigned during running
    private long currentDuration = 0;
    private long duration_long = 0;

    //Returned chunk proto
    private VideoChunk videoChunk;

    /**
     * Constructor of VideoUploadTask.
     * @param videoId
     * @param tsPath
     * @param tsAbsolutePathPath
     * @param endTag
     */
    public VideoUploadTask(String videoId, String tsPath, String tsAbsolutePathPath, boolean endTag){
        this.videoId = videoId;
        this.tsPath = tsPath;
        this.tsAbsolutePath = tsAbsolutePathPath;
        this.endTag = endTag;
    }

    public VideoUploadTask(String videoId, String tsPath, String tsAbsolutePathPath, long duration, boolean endTag){
        this(videoId, tsPath, tsAbsolutePathPath, endTag);
        duration_long = duration;
    }

    public boolean isEnd(){
        return endTag;
    }

    /**
     * This handler is called by ipfsAddData
     * It does the following things:<br />
     *  - create VideoChunk proto object.
     */
    private Handlers.IpfsAddDataHandler tsHandler = new Handlers.IpfsAddDataHandler() {
        @Override
        public void onComplete(String path) {
            synchronized (LOCK) {
                Log.d(TAG, String.format("IPFS add complete for file %s with ipfs path: %s", tsPath, path));
                videoChunk = VideoChunk.newBuilder()
                        .setId(videoId)
                        .setChunk(tsPath)
                        .setAddress(path)
                        .setStartTime(currentDuration)
                        .setEndTime(currentDuration + duration_long)
                        .build();

                Log.d(TAG, "Chunk proto built.");
                LOCK.notify();
            }
        }

        @Override
        public void onError(Exception e) {
            synchronized (LOCK) {
                Log.e(TAG, String.format("Unexpect ipfs error when add file %s", tsPath));
                e.printStackTrace();
                LOCK.notify();
            }
        }
    };

    /**
     * Read the duration info from the ts file.
     * This method is exactly the way we read duration for videoMeta.
     * It may take a bit time and the duration may be slightly different from the m3u8 list.
     * @return Duration in us (1/1000 ms)
     * @TODO int32 is not enough for time in us.
     */
    private int readDurationFromReceiver(){
        Log.d(TAG, String.format("Extract task %s duration from ts file using receiver.", tsPath));
        timeLog.begin();
        MediaMetadataRetriever mdataReceiver = null;
        mdataReceiver = new MediaMetadataRetriever();
        mdataReceiver.setDataSource(tsAbsolutePath);
        String duration = mdataReceiver.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        mdataReceiver.release();
        Log.d(TAG, String.format("Extract task %s duration and content use %d ms.", tsPath, timeLog.stopGetTime()));
        return Integer.parseInt(duration) * 1000;
    }

    /**
     * Get the end video upload task. Video task is processed through block queue.
     * This task will notify the queue to stop.
     * @return An virtual upload task to notify uploader to stop.
     */
    public static VideoUploadTask endTask(){
        return new VideoUploadTask("","", "",true);
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
    public VideoChunk upload(long currentDuration) throws VideoExceptions.UnexpectedEndException{
        this.currentDuration = currentDuration;
        //duration_int = readDurationFromReceiver();
        if(endTag){
            Log.d(TAG, "End task received. Return -1 to end the task thread.");
            throw new VideoExceptions.UnexpectedEndException();
        }

        try {
            Log.d(TAG, String.format("Add task %s to ipfs.", tsPath));
            timeLog.begin();
            byte[] fileContent = Files.readAllBytes(Paths.get(tsAbsolutePath));
            synchronized (LOCK) {
                Textile.instance().ipfs.ipfsAddData(fileContent, true, false, tsHandler);
                Log.d(TAG, "Task wait for ipfs complete");
                LOCK.wait();
                Log.d(TAG, "Task notified");
            }
            Log.d(TAG, String.format("IPFS add complete. Use time %d ms", timeLog.stopGetTime()));
        }catch(IOException ie){
            Log.e(TAG, "Unexpected io exception when read ts contents.");
            ie.printStackTrace();
        }catch(InterruptedException ie){
            Log.e(TAG, "Unexpected interruption when run ipfs add.");
            ie.printStackTrace();
        }

        return videoChunk;
    }
}
