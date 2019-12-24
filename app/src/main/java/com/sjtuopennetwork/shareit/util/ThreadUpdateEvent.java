package com.sjtuopennetwork.shareit.util;

import sjtu.opennet.hon.FeedItemData;

public class ThreadUpdateEvent {
    public String threadId;
    public FeedItemData feedItemData;

    public ThreadUpdateEvent(String threadId, FeedItemData feedItemData){
        this.threadId=threadId;
        this.feedItemData=feedItemData;
    }
}
