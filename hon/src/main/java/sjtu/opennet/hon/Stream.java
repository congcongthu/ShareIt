package sjtu.opennet.hon;

import android.util.Log;

import mobile.Mobile_;
import sjtu.opennet.textilepb.Model;

public class Stream extends NodeDependent{

    private static final String TAG = "Stream:==============";

    public Stream(Mobile_ node) {
        super(node);
    }

    public void startStream(String threadId, Model.StreamMeta streammeta) throws Exception {
        Log.d(TAG, "startStream,streamid: "+streammeta.getId());
        node.startStream(threadId,streammeta.toByteArray());
    }

    public void subscribeStream(String streamid) throws Exception{
        Log.d(TAG, "subscribeStream,streamid: "+streamid);
        node.subscribeStream(streamid); //temp
    }

    public void streamAddFile(String streamid, byte[] streamFile) throws Exception{
        Log.d(TAG, "streamAddFile,streamid: "+streamid);
        node.streamAddFile(streamid, streamFile);
    }

}
