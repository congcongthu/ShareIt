package sjtu.opennet.hon;

import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;

import java.util.Date;

import sjtu.opennet.textilepb.View.Announce;
import sjtu.opennet.textilepb.View.Comment;
import sjtu.opennet.textilepb.View.FeedItem;
import sjtu.opennet.textilepb.View.Files;
import sjtu.opennet.textilepb.View.Ignore;
import sjtu.opennet.textilepb.View.Join;
import sjtu.opennet.textilepb.View.Leave;
import sjtu.opennet.textilepb.View.Like;
import sjtu.opennet.textilepb.View.Text;


public class Util {

    public static Date timestampToDate(Timestamp timestamp) {
        double milliseconds = timestamp.getSeconds() * 1e3 + timestamp.getNanos() / 1e6;
        return new Date((long)milliseconds);
    }

    static FeedItemData feedItemData(FeedItem feedItem) throws Exception {
        FeedItemData feedItemData;
        String typeUrl = feedItem.getPayload().getTypeUrl();
        ByteString bytes = feedItem.getPayload().getValue();

        feedItemData = new FeedItemData();
        feedItemData.block = feedItem.getBlock();
        switch (typeUrl) {
            case "/Text":
                feedItemData.type = FeedItemType.TEXT;
                feedItemData.text = Text.parseFrom(bytes);
                break;
            case "/Comment":
                feedItemData.type = FeedItemType.COMMENT;
                feedItemData.comment = Comment.parseFrom(bytes);
                break;
            case "/Like":
                feedItemData.type = FeedItemType.LIKE;
                feedItemData.like = Like.parseFrom(bytes);
                break;
            case "/Files":
                feedItemData.type = FeedItemType.FILES;
                feedItemData.files = Files.parseFrom(bytes);
                break;
            case "/Ignore":
                feedItemData.type = FeedItemType.IGNORE;
                feedItemData.ignore = Ignore.parseFrom(bytes);
                break;
            case "/Join":
                feedItemData.type = FeedItemType.JOIN;
                feedItemData.join = Join.parseFrom(bytes);
                break;
            case "/Leave":
                feedItemData.type = FeedItemType.LEAVE;
                feedItemData.leave = Leave.parseFrom(bytes);
            case "/Announce":
                feedItemData.type = FeedItemType.ANNOUNCE;
                feedItemData.announce = Announce.parseFrom(bytes);
                break;
            default:
                throw new Exception("Unknown feed item typeUrl: " + typeUrl);
        }
        return feedItemData;
    }
}
