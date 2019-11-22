package sjtu.opennet.honvideo;

import android.util.Log;

/**
 * Time log is used to calculate and print time used during code running.
 * We specifically construct this class in order to: <br />
 *  - Make code more clear.<br />
 *  - Let debug and release version performed differently. We propose an empty TimeLog class in release sourceset.
 *    Which means that the release version of this package and the app built wouldn't waste time to record time.
 */
public class TimeLog {
    private long beginTime = 0;
    private long stopTime = 0;
    private String TAG;

    TimeLog(String tag){
        TAG = tag;
    }

    public void begin(){
        beginTime = System.currentTimeMillis();
    }

    public void stop(){
        stopTime = System.currentTimeMillis();
    }

    public void stopPrint(){
        stop();
        print();
    }

    public long stopGetTime(){
        stop();
        return processTime();
    }

    public void print(){
        Log.d(TAG, String.format("Process Time %d MS", processTime()));
    }

    public long processTime(){
        if(beginTime == 0 || stopTime == 0){
            Log.e("TimeLog", "Neither begin nor stop time has not been assigned.");
            return 0;
        }
        return stopTime - beginTime;
    }
}
