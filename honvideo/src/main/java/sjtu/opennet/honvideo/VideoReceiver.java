package sjtu.opennet.honvideo;

import android.util.Log;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

/**
 * @TODO stop the thread using stop flag so that we can safely stop this thread when activity is destroy. Another way to do this is add a special end task with highest priority.
 * @TODO use local DB to replace chunklist.
 */
public class VideoReceiver extends Thread{
    private final String TAG = "HONVIDEO.VideoReceiver";

    private BlockingQueue<VideoReceiveTask> vQueue;
    private String videoPath;
    private String chunkPath;
    private boolean complete = false;
    private boolean stop = false;

    private String m3u8Path;
    private String chunkListPath;
    private Set<Integer> chunkIndexSet;
    private int currentIndex = -1;
    public VideoReceiver(BlockingQueue<VideoReceiveTask> vQueue, String videoPath, String chunkPath){
        this.vQueue = vQueue;
        this.videoPath = videoPath;
        this.chunkPath = chunkPath;

        this.m3u8Path = String.format("%s/playList.m3u8", videoPath);
        this.chunkListPath = String.format("%s/chunkList.txt", videoPath);

        chunkIndexSet = new HashSet<>();
        resumeState();
    }

    /**
     * resume state from chunklist and m3u8
     */
    private void resumeState(){
        /**
         * First read the list and m3u8 file.
         */

        /**
         * If they don't exist, create new ones.
         */
        if(!FileUtil.fileExists(m3u8Path)) {
            Log.d(TAG, String.format("m3u8 not exist. create %s.", m3u8Path));
            FileUtil.createNewFile(m3u8Path);
            initM3u8();
        }
        if(!FileUtil.fileExists(chunkListPath)) {
            Log.d(TAG, String.format("list not exist. create %s.", chunkListPath));
            FileUtil.createNewFile(chunkListPath);
        }
    }

    private void initM3u8(){
        String head="#EXTM3U\n" +
                "#EXT-X-VERSION:3\n" +
                "#EXT-X-MEDIA-SEQUENCE:0\n" +
                "#EXT-X-ALLOW-CACHE:YES\n" +
                "#EXT-X-TARGETDURATION:15\n" +
                "#EXT-X-PLAYLIST-TYPE:EVENT\n";
        FileUtil.appendToFileWithNewLine(m3u8Path, head);
    }

    /**
     * updateState does following things:<br />
     *  - Add chunk index to chunk index list and list file.
     *  - Update currentIndex if the index if successor of currentIndex
     *  - Write to m3u8 file if currentIndex is updated.
     *  - Further update currentIndex and m3u8 if there is more successors in chunk index list.
     */
    private void updateState(int tmpIndex){
        if(!chunkIndexSet.contains(tmpIndex)){
            chunkIndexSet.add(tmpIndex);
            FileUtil.appendToFileWithNewLine(chunkListPath, Integer.toString(tmpIndex));
        }
        updateRecursive(tmpIndex);
    }

    private void updateRecursive(int tmpIndex){
        if(tmpIndex == (currentIndex + 1)){
            currentIndex = tmpIndex;
            updateM3u8(tmpIndex);
            if(chunkIndexSet.contains(currentIndex + 1)){
                updateRecursive(currentIndex + 1);
            }
        }
    }

    /**
     * @TODO Complete this function according to the requirement of media player.
     * @param tmpIndex
     */
    private void updateM3u8(int tmpIndex){
        /*
        #EXTINF:4.700489,
                out0000.ts

         */
        return;
    }

    public void shutDown(){
        Log.w(TAG, "Video Receiver Shut Down!!\nNote that this is not the normal exit method.");
        interrupt();
    }

    @Override
    public void run(){
        Log.d(TAG, "Receiver start to run.");
        VideoReceiveTask vTask;
        while(!complete) {
            try {
                vTask = vQueue.take();
                if(vTask.isEnd()){
                    Log.d(TAG, "End Task received. End the thread.");
                    complete = true;
                }else{
                    //
                    //DO SOMETHING
                    //Judeg whether to process task
                    String fileName = vTask.getFileName();
                    //String fileName = vTask.process();
                    int tmpIndex = Segmenter.getIndFromPath(fileName);
                    if(chunkIndexSet.contains(tmpIndex)){
                        Log.d(TAG, String.format("File %s already received.", fileName));
                        updateState(tmpIndex);
                    }else{
                        Log.d(TAG, String.format("Task with file %s start.", fileName));
                        boolean success = vTask.process();
                        if(!success){
                            Thread.sleep(1000);
                            vQueue.add(vTask);
                            Log.e(TAG, String.format("Add the failed task %s back to queue", fileName));
                        }else{
                            Log.d(TAG, "Task success. Update Receiver State.");
                            updateState(tmpIndex);
                        }
                    }

                    //
                }
            } catch (InterruptedException ie) {
                Log.e(TAG, "Unexpected Interrupt.");
                ie.printStackTrace();
                interrupt();
            } catch(Exception e){
                e.printStackTrace();
            }
            //videoQueue.notify();
        }
        Log.d(TAG, "Receiver end safely.");
    }
}
