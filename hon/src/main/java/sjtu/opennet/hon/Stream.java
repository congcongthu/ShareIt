package sjtu.opennet.hon;

import android.util.Log;

import java.util.logging.Handler;

import mobile.Mobile_;
import sjtu.opennet.textilepb.Model;
import sjtu.opennet.textilepb.View;

public class Stream extends NodeDependent{

    private static final String TAG = "Stream:==============";

    public Stream(Mobile_ node) {
        super(node);
    }

    public void startStream(String threadId, Model.StreamMeta streamMeta) throws Exception {
        Log.d(TAG, "startStream,streamid: "+streamMeta.getId());
        node.startStream_Text(threadId,streamMeta.toByteArray());
    }

    public void subscribeStream(String streamId) throws Exception{
        Log.d(TAG, "subscribeStream,streamid: "+streamId);
        node.subscribeStream(streamId); //temp
    }

    public void streamAddFile(String streamId, byte[] streamFile) throws Exception{
        Log.d(TAG, "streamAddFile,streamid: "+streamId);
        node.streamAddFile(streamId, streamFile);
    }

    public Model.StreamMeta fileAsStream(String threadId, Model.StreamFile streamFile, Model.StreamMeta.Type fileType) throws Exception {
        Log.d(TAG, "fileAsStream");
        return Model.StreamMeta.parseFrom(node.fileAsStream_Text(threadId, streamFile.toByteArray(), fileType.getNumber()));
    }

    public void closeStream(String threadId, String streamId) throws Exception{
        Log.d(TAG, "closeStream: "+streamId);
        node.closeStream(threadId, streamId);
    }

    public void threadAddStream(String threadId, String streamId) throws Exception{

    }

    public void setDegree(int num){
        node.setMaxWorkers((long)num);
    }

    public long getWorker(){
        return node.getMaxWorkers();
    }

    public void dataAtStreamFile(View.FeedStreamMeta feed,String hash, final Handlers.DataHandler handler) {
        node.dataAtStreamFile(feed.toByteArray(),hash.getBytes(), (data, media, e)->{
            if (e != null) {
                handler.onError(e);
                return;
            }
            try {
                handler.onComplete(data, media);
            } catch (final Exception exception) {
                handler.onError(exception);
            }
        });
    }

}
