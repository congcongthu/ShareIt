package sjtu.opennet.honvideo;


import android.media.MediaMetadataRetriever;
import android.util.Log;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.BlockingQueue;

import sjtu.opennet.hon.Handlers;
import sjtu.opennet.hon.Textile;
import sjtu.opennet.textilepb.Model.VideoChunk;

/**
 * Video upload task.
 *
 */

public class VideoUploadTask {
    private final String TAG = "HONVIDEO.VideoUploadTask";
    //private boolean ipfsComplete = false;

    //Variables get from constructor
    private String videoId;
    private String tsPath;
    private String tsAbsolutePath;
    private BlockingQueue<ChunkPublishTask> chunkQueue;
    private boolean endTag;

    //Variables assigned during running
    private int currentDuration = 0;
    private int duration_int = 0;
    private BlockingQueue<Integer> blockQueue;

    public VideoUploadTask(String videoId, String tsPath, String tsAbsolutePathPath, BlockingQueue<ChunkPublishTask> chunkQueue, boolean endTag){
        this.videoId = videoId;
        this.tsPath = tsPath;
        this.tsAbsolutePath = tsAbsolutePathPath;
        this.chunkQueue = chunkQueue;
        this.endTag = endTag;
    }

    public String getChunkName(){
        if(endTag){
            return "ENDTASK";
        }
        return tsPath;
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
            Log.d(TAG, String.format("IPFS add complete for file %s with ipfs path: %s", tsPath, path));
            //ipfsComplete = true;
            VideoChunk videoChunk = VideoChunk.newBuilder()
                    .setId(videoId)
                    .setChunk(tsPath)
                    .setAddress(path)
                    .setStartTime(currentDuration)
                    .setEndTime(currentDuration + duration_int)
                    .build();
            Log.d(TAG, String.format("Add task %s to Publish Queue.", tsPath));
            chunkQueue.add(new ChunkPublishTask(videoChunk, false));
            blockQueue.add(1);
//            int tmpSize = chunkQueue.size();
//            Log.d(TAG, String.format("Size of chunk queue: %s", tmpSize));
//            try {
//                Textile.instance().videos.addVideoChunk(videoChunk);
//                Textile.instance().videos.publishVideoChunk(videoChunk);
//            }catch(Exception e){
//                Log.e(TAG, "Unexpected error when publish video chunk");
//                e.printStackTrace();
//            }
        }

        @Override
        public void onError(Exception e) {
            Log.e(TAG, String.format("Unexpect ipfs error when add file %s", tsPath));
            blockQueue.add(-1);
            e.printStackTrace();
        }
    };

    public static VideoUploadTask endTask(){
        return new VideoUploadTask("","", "", null,true);
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
    public int upload(int currentDuration, BlockingQueue<Integer> blockQueue){
        long currentTime;
        long newTime;
        this.blockQueue = blockQueue;
        this.currentDuration = currentDuration;
        if(endTag){
            Log.d(TAG, "End task received. Return -1 to end the task thread.");
            blockQueue.add(0);
            return -1;
        }
        //Log.d(TAG, String.format("Video Upload Task Begin, Chunk Start Duration %d", currentDuration));
        //int duration_int = 0;
        MediaMetadataRetriever mdataReceiver = null;
        try {

            Log.d(TAG, String.format("Extract task %s duration and content.", tsPath));
            currentTime = System.currentTimeMillis();
            mdataReceiver = new MediaMetadataRetriever();
            mdataReceiver.setDataSource(tsAbsolutePath);
            String duration = mdataReceiver.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            byte[] fileContent = Files.readAllBytes(Paths.get(tsAbsolutePath));
            newTime = System.currentTimeMillis();
            Log.d(TAG, String.format("Extract task %s duration and content use %d MS.", tsPath, newTime - currentTime));
            currentTime = newTime;


            if (duration == null) {
                Log.w(TAG, "Can not extract dutation.");
                duration_int = -1;
                blockQueue.add(-1);
                return -1;
            } else {
                duration_int = Integer.parseInt(duration);
                Log.d(TAG, String.format("Add task %s to ipfs.", tsPath));
                Textile.instance().ipfs.ipfsAddData(fileContent, true, false, tsHandler);
            }
            //Log.d(TAG, String.format("Video Upload Task Done, Chunck End Duration: %d", currentDuration + duration_int));

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
