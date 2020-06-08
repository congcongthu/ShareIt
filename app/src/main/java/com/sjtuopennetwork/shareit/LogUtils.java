package com.sjtuopennetwork.shareit;

public class LogUtils {
    private static boolean isDebug = true;
    private static String TAG = "tag";

    public boolean isDebug() {
        return isDebug;
    }

    public static void setDebug(boolean debug) {
        isDebug = debug;
    }
    public static void setTag(String debug) {
        TAG = debug;
    }
    public static void e(String msg) {
        if (isDebug) {
            LogUtils.e(TAG,msg);
        }
    }
    public static void v(String msg) {
        if (isDebug) {
            LogUtils.v(TAG,msg);
        }
    }
    public static void i(String msg) {
        if (isDebug) {
            LogUtils.i(TAG,msg);
        }
    }
    public static void d(String msg) {
        if (isDebug) {
            LogUtils.d(TAG,msg);
        }
    }
    public static void w(String msg) {
        if (isDebug) {
            LogUtils.w(TAG,msg);
        }
    }
    //tag is a param
    public static void e(String tag,String msg) {
        if (isDebug) {
            LogUtils.e(tag,msg);
        }
    }
    public static void d(String tag,String msg) {
        if (isDebug) {
            LogUtils.d(tag,msg);
        }
    }
    public static void v(String tag,String msg) {
        if (isDebug) {
            LogUtils.v(tag,msg);
        }
    }
    public static void w(String tag,String msg) {
        if (isDebug) {
            LogUtils.w(tag,msg);
        }
    }
    public static void i(String tag,String msg) {
        if (isDebug) {
            LogUtils.i(tag,msg);
        }
    }
}
