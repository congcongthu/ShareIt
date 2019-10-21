package com.sjtuopennetwork.shareit.util;

public class MyEvent {
    private int code;
    private Object event;

    public MyEvent(int code, Object event) {
        this.code = code;
        this.event = event;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public Object getEvent() {
        return event;
    }

    public void setEvent(Object event) {
        this.event = event;
    }
}
