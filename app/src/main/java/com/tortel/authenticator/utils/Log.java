package com.tortel.authenticator.utils;

/**
 * Log wrapper that uses a fixed tag
 */
public class Log {
    private static final String TAG = "auth+";

    public static void d(String m){
        android.util.Log.d(TAG, m);
    }

    public static void v(String m){
        android.util.Log.v(TAG, m);
    }

    public static void e(String m){
        e(m, null);
    }

    public static void e(String m, Throwable th){
        android.util.Log.e(TAG, m, th);
    }
}
