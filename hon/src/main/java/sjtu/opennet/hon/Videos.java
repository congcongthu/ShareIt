package sjtu.opennet.hon;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import mobile.Mobile_;
import mobile.SearchHandle;
import sjtu.opennet.textilepb.Model.Video;
import sjtu.opennet.textilepb.Model.VideoChunk;
import sjtu.opennet.textilepb.Model.VideoChunkList;
import sjtu.opennet.textilepb.QueryOuterClass.QueryOptions;
import sjtu.opennet.textilepb.QueryOuterClass.VideoChunkQuery;

/**
 * Provides access to Textile threads related APIs
 */
public class Videos extends NodeDependent {

    private Lock lock = new ReentrantLock();
    Videos(final Mobile_ node) {
        super(node);
    }

    public Video getVideo(final String videoId) throws Exception {
        final byte[] bytes = node.getVideo(videoId);
        return Video.parseFrom(bytes);
    }
    
    public VideoChunk getVideoChunk(final String videoId, final String chunk) throws Exception {
        final byte[] bytes = node.getVideoChunk(videoId, chunk);
        try {
            return VideoChunk.parseFrom(bytes);
        }catch (NullPointerException e){
            return null;
        }
    }

    public void addVideo(final Video video) throws Exception {
        node.addVideo(video.toByteArray());
    }

    public void addVideoChunk(final VideoChunk vchunk) throws Exception {
        node.addVideoChunk(vchunk.toByteArray());
    }

    public void threadAddVideo(final String threadId, final String videoId) throws Exception {
        node.threadAddVideo(threadId, videoId);
    }

    public void publishVideo(final Video video) throws Exception {
        node.publishVideo(video.toByteArray());
    }

    public void publishVideoChunk(final VideoChunk vchunk) throws Exception {
//        lock.lock();
        node.publishVideoChunk(vchunk.toByteArray());
//        lock.unlock();
    }
 
    public VideoChunkList chunksByVideoId(final String videoId) throws Exception {
        final byte[] bytes = node.chunksByVideoId(videoId);
        return VideoChunkList.parseFrom(bytes);
    }
    /**
     * Searches the network for video chunks
     * @param query The object describing the query to execute
     * @param options Options controlling the behavior of the search
     * @return A handle that can be used to cancel the search
     * @throws Exception The exception that occurred
     */
    public SearchHandle searchVideoChunks(final VideoChunkQuery query, final QueryOptions options) throws Exception {
        return node.searchVideoChunks(query.toByteArray(), options.toByteArray());
    }
}
