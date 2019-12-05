package sjtu.opennet.honvideo;

import sjtu.opennet.textilepb.Model;

public class VideoHandlers {

    /**
     * Interface representing an object that can be
     * called with the Video Receiver result.
     */
    public interface ReceiveHandler {
        //void onComplete(Model.Video videoPb);
        void onChunkComplete(Model.VideoChunk vChunk);
        void onVideoComplete();
        void onError(final Exception e);
    }

    public interface SearchResultHandler{
        void onGetAnResult(Model.VideoChunk vChunk, boolean isEnd);
        void onError(final Exception e);
    }

    public interface UploadHandler {
        void onPublishComplete();
    }

    public final static String chunkEndTag = "VIRTUAL";
}
