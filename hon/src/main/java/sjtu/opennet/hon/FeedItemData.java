package sjtu.opennet.hon;

import sjtu.opennet.textilepb.View.Announce;
import sjtu.opennet.textilepb.View.Comment;
import sjtu.opennet.textilepb.View.Ignore;
import sjtu.opennet.textilepb.View.Join;
import sjtu.opennet.textilepb.View.Leave;
import sjtu.opennet.textilepb.View.Like;
import sjtu.opennet.textilepb.View.Text;
import sjtu.opennet.textilepb.View.Files;

public class FeedItemData {
    public FeedItemType type;
    public String block;
    public Text text;
    public Comment comment;
    public Like like;
    public Files files;
    public Ignore ignore;
    public Join join;
    public Leave leave;
    public Announce announce;
}
