package sjtu.opennet.hon;

import mobile.Mobile_;
import sjtu.opennet.textilepb.Model;
import sjtu.opennet.textilepb.View;

/**
 * Provides access to Textile IPFS related APIs
 */
public class Ipfs extends NodeDependent {

    Ipfs(final Mobile_ node) {
        super(node);
    }

    /**
     * Fetch the IPFS peer id
     * @return The IPFS peer id of the local Textile node
     * @throws Exception The exception that occurred
     */
    public String peerId() throws Exception {
        return node.peerId();
    }

    /**
     * Open a new direct connection to a peer using an IPFS multiaddr
     * @param multiaddr Peer IPFS multiaddr
     * @return Whether the peer swarm connect was successfull
     * @throws Exception The exception that occurred
     */
    public Boolean swarmConnect(final String multiaddr) throws Exception {
        final String result = node.swarmConnect(multiaddr);
        return result.length() > 0;
    }

    public String getSwarmAddress(final String peerId) throws  Exception {
        return node.getSwarmAddress(peerId);
    }

    public Model.SwarmPeerList connectedAddresses() throws  Exception {
        final byte[] bytes = node.connectedAddresses();
        return Model.SwarmPeerList.parseFrom(bytes);
    }

    /**
     * Get raw data stored at an IPFS path
     * @param path The IPFS path for the data you want to retrieve
     * @param handler An object that will get called with the resulting data and media type
     */
    public void dataAtPath(final String path, final Handlers.DataHandler handler) {
        node.dataAtPath(path, (data, media, e) -> {
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

    public void dataAtFeedSimpleFile(View.FeedSimpleFile feed, final Handlers.DataHandler handler) {
        node.dataAtFeedSimpleFile(feed.toByteArray(), (data, media, e)->{
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
    
    /**
     * Add raw data to IPFS
     * @param data Raw data to be added
     * @param pin Whether or not to pin it
     * @param hashOnly Whether or not only hash it
     * @param handler An object that will get called with the resulting data and media type
     */
    public void ipfsAddData(final byte[] data, boolean pin, boolean hashOnly, final Handlers.IpfsAddDataHandler handler) {
        node.ipfsAddData(data, pin, hashOnly, (path, e) -> {
            if (e != null) {
                handler.onError(e);
                return;
            }
            try {
                handler.onComplete(path);
            } catch (final Exception exception) {
                handler.onError(exception);
            }
        });
    }
}
