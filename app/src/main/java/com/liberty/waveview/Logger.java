package com.liberty.waveview;

import android.content.Context;

import java.util.Date;

/**
 * custom logger class, please use these application wide to replace default android log if possible.
 */
public final class Logger {
    private static final int MAX_TAG_LENGTH = 23;
    private static String appTagPrefix = "O2.";
    public static boolean DEBUG = true;

    public static final String O2_DEBUG = "o2.debug";
 
    private static final String ORIGINAL_CLASS_FULL_NAME = "com.oazon.common.Logger";
    
    /**
     * is currently debug version or not.
     * note: don't change this field from outside.
     * note: we assume this class is obscured in release version.
     */
    public static boolean isDebugVersion;

    // current logger level, used to filter out too many log outputs in release version.
    private static int currentLevel = android.util.Log.DEBUG;
    
    // accelerate for get current timestamp (less memory alloc, a little bit quicker)
    // base total millis is 00:00:00 at today.
    private static long baseTotalMillisOfToday;
            
    private Logger() {}

    public static void enableDebug() {
        DEBUG = true;
    }

    public static void disableDebug() {
        DEBUG = false;
    }

    public static  void restoreDebugState(Context context) {
        if (BuildConfig.DEBUG) {
            DEBUG = true;
        }
    }
    // this will add a prefix for all TAG used in this application.
    // eg.
    //   Logger.init("++ ");
    //   then ` adb logcat | grep ++ ` will easily list exactly only your logs
    // NOTE: android TAG max length is 23 chars, so make sure use a short prefix
    public static void init(String tagPrefixOfApp) {
        appTagPrefix = tagPrefixOfApp;

        initNow();
        
        try {
            // NOTE: here we assume release version will have this Logger class obscured.
            Class.forName(ORIGINAL_CLASS_FULL_NAME);
            isDebugVersion = true;
        } catch (ClassNotFoundException e) {
            isDebugVersion = false;
        }

        if (isDebugVersion)
            currentLevel = android.util.Log.VERBOSE;
        
        Logger.i("", "Logger Started, DebugVersion = " + isDebugVersion);
    }

    private static String categoryToTag(String category) {
        return appTagPrefix + category;
    }

    public static String getStackTraceString(Throwable tr) {
        if (!DEBUG)
            return  "";
        return android.util.Log.getStackTraceString(tr);
    }

    public static void write(int level, String category, String msg) {
        write(level, category, msg, null);
    }

    public static void write(int level, String category, String msg, Throwable tr) {
        if (!DEBUG)
            return;
        // don't output this log if its level is lower than current level
        if (level < currentLevel)
            return;
        
        String tag = categoryToTag(category);
        if (tag.length() > MAX_TAG_LENGTH)   // android log doesn't support more than 23 chars in TAG
            tag = tag.substring(0, MAX_TAG_LENGTH);
        if (tr == null) {
            android.util.Log.println(level, tag, String.format("%s[%d] %s", now(), Thread.currentThread().getId(), msg));
        } else {
            android.util.Log.println(level, tag, String.format("%s[%d] %s - %s", now(), Thread.currentThread().getId(), msg, Logger.getStackTraceString(tr)));
        }
    }

    private static void initNow() {
        long totalMillis = System.currentTimeMillis();
        Date now = new Date();
        baseTotalMillisOfToday = totalMillis - totalMillis % 1000L -  
                ((now.getHours() * 60L + now.getMinutes()) * 60L + now.getSeconds()) * 1000L; 
    }
    
    // get now as string, it's multiple-thread safe
    private static String now() {
        long delta = System.currentTimeMillis() - baseTotalMillisOfToday;
        int hours = (int) (delta / (3600L * 1000L)) % 24;
        int minutes = (int) (delta / (60L * 1000L)) % 60;
        int seconds = (int) (delta / 1000L) % 60;
        int millis = (int) (delta % 1000L);
        return String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, millis);
    }

    public static void v(String category, String msg) {
        write(android.util.Log.VERBOSE, category, msg);
    }

    public static void v(String category, String msg, Throwable tr) {
        write(android.util.Log.VERBOSE, category, msg, tr);
    }

    public static void v(String category, String format, Object... args) {
        write(android.util.Log.VERBOSE, category, String.format(format, args));
    }

    public static void d(String category, String msg) {
        write(android.util.Log.DEBUG, category, msg);
    }

    public static void d(String category, String msg, Throwable tr) {
        write(android.util.Log.DEBUG, category, msg, tr);
    }

    public static void d(String category, String format, Object... args) {
        write(android.util.Log.DEBUG, category, String.format(format, args));
    }

    public static void i(String category, String msg) {
        write(android.util.Log.INFO, category, msg);
    }

    public static void i(String category, String format, Object... args) {
        write(android.util.Log.INFO, category, String.format(format, args));
    }

    public static void w(String category, String msg) {
        write(android.util.Log.WARN, category, msg);
    }

    public static void w(String category, Throwable tr) {
        write(android.util.Log.WARN, category, getStackTraceString(tr));
    }

    public static void w(String category, String msg, Throwable tr) {
        write(android.util.Log.WARN, category, msg, tr);
    }

    public static void e(String category, String msg) {
        write(android.util.Log.ERROR, category, msg);
    }

    public static void e(String category, Throwable tr) {
        write(android.util.Log.ERROR, category, getStackTraceString(tr));
    }

    public static void e(String category, String msg, Throwable tr) {
        write(android.util.Log.ERROR, category, msg, tr);
    }

    public static void f(String category, String msg) {
        write(android.util.Log.ASSERT, category, msg);
    }

    public static void f(String category, Throwable tr) {
        write(android.util.Log.ASSERT, category, getStackTraceString(tr));
    }

    public static void f(String category, String msg, Throwable tr) {
        write(android.util.Log.ASSERT, category, msg, tr);
    }
}
