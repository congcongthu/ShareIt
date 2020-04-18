package sjtu.opennet.hon;

import android.util.Log;

import mobile.Mobile_;
import sjtu.opennet.textilepb.Model;

public class Stream extends NodeDependent{

    private static final String TAG = "Stream:==============";

    public Stream(Mobile_ node) {
        super(node);
    }

    public void startStream(String threadId, Model.StreamMeta streamMeta) throws Exception {
        Log.d(TAG, "startStream,streamid: "+streamMeta.getId());
        node.startStream(threadId,streamMeta.toByteArray());
    }

    public void subscribeStream(String streamId) throws Exception{
        Log.d(TAG, "subscribeStream,streamid: "+streamId);
        node.subscribeStream(streamId); //temp
    }

    public void streamAddFile(String streamId, byte[] streamFile) throws Exception{
        Log.d(TAG, "streamAddFile,streamid: "+streamId);
        node.streamAddFile(streamId, streamFile);
    }

    public void closeStream(String threadId, String streamId) throws Exception{
        Log.d(TAG, "closeStream: "+streamId);
        node.closeStream(threadId, streamId);
    }
}
