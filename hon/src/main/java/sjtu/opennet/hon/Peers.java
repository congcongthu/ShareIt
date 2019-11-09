package sjtu.opennet.hon;

import mobile.Mobile_;
import mobile.SearchHandle;
import sjtu.opennet.textilepb.Model.User;

/**
 * Provides access to Textile threads related APIs
 */
public class Peers extends NodeDependent {

    Peers(final Mobile_ node) {
        super(node);
    }

    public User bestUser(final String peerId) throws Exception {
        final byte[] bytes = node.bestUser(peerId);
        return User.parseFrom(bytes);
    }
    
}
