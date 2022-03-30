package com.yu.lib.video.library.utils;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

public class VLog {
    private static final String TAG = "VideoLog";

    private static boolean DEBUG;
    private static final SimpleDateFormat sDateFormat = new SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss.SSS");
    private static final boolean WITH_FILE_INFO = false;

    public static void setDebug(boolean debug) {
        DEBUG = debug;
    }

    public static boolean isDebug() {
        return DEBUG;
    }

    public static void d(String tag, String msg) {
        if (DEBUG) {
            android.util.Log.d(getFinalTag(tag), getFinalLog(msg));
        }
    }

    public static void d(String tag, Throwable e) {
        if (DEBUG) {
            d(tag, "", e);
        }
    }

    public static void d(String tag, String format, Object arg1) {
        if (DEBUG) {
            android.util.Log.d(getFinalTag(tag), getFinalLog(logFormat(format, arg1)));
        }
    }

    public static void d(String tag, String format, Object arg1, Object arg2) {
        if (DEBUG) {
            android.util.Log.d(getFinalTag(tag), getFinalLog(logFormat(format, arg1, arg2)));
        }
    }

    public static void d(String tag, String format, Object arg1, Object arg2, Object arg3) {
        if (DEBUG) {
            android.util.Log.d(getFinalTag(tag), getFinalLog(logFormat(format, arg1, arg2, arg3)));
        }
    }

    public static void d(String tag, String format, Object... args) {
        if (DEBUG) {
            android.util.Log.d(getFinalTag(tag), getFinalLog(logFormat(format, args)));
        }
    }

    public static void w(String tag, String msg) {
        if (DEBUG) {
            android.util.Log.w(getFinalTag(tag), getFinalLog(msg));
        }
    }

    public static void w(String tag, Throwable e) {
        if (DEBUG) {
            w(tag, "", e);
        }
    }

    public static void w(String tag, String format, Object arg1) {
        if (DEBUG) {
            android.util.Log.w(getFinalTag(tag), getFinalLog(logFormat(format, arg1)));
        }
    }

    public static void w(String tag, String format, Object arg1, Object arg2) {
        if (DEBUG) {
            android.util.Log.w(getFinalTag(tag), getFinalLog(logFormat(format, arg1, arg2)));
        }
    }

    public static void w(String tag, String format, Object arg1, Object arg2, Object arg3) {
        if (DEBUG) {
            android.util.Log.w(getFinalTag(tag), getFinalLog(logFormat(format, arg1, arg2, arg3)));
        }
    }

    public static void w(String tag, String format, Object... args) {
        if (DEBUG) {
            android.util.Log.w(getFinalTag(tag), getFinalLog(logFormat(format, args)));
        }
    }

    public static void e(String tag, String msg) {
        if (DEBUG) {
            android.util.Log.e(getFinalTag(tag), getFinalLog(msg));
        }
    }

    public static void e(String tag, Throwable e) {
        if (DEBUG) {
            e(tag, "", e);
        }
    }

    public static void e(String tag, String format, Object arg1) {
        if (DEBUG) {
            android.util.Log.e(getFinalTag(tag), getFinalLog(logFormat(format, arg1)));
        }
    }

    public static void e(String tag, String format, Object arg1, Object arg2) {
        if (DEBUG) {
            android.util.Log.e(getFinalTag(tag), getFinalLog(logFormat(format, arg1, arg2)));
        }
    }

    public static void e(String tag, String format, Object arg1, Object arg2, Object arg3) {
        if (DEBUG) {
            android.util.Log.e(getFinalTag(tag), getFinalLog(logFormat(format, arg1, arg2, arg3)));
        }
    }

    public static void e(String tag, String format, Object... args) {
        if (DEBUG) {
            android.util.Log.e(getFinalTag(tag), getFinalLog(logFormat(format, args)));
        }
    }

    public static void v(String tag, String msg) {
        if (DEBUG) {
            android.util.Log.v(getFinalTag(tag), getFinalLog(msg));
        }
    }

    public static void v(String tag, Throwable e) {
        if (DEBUG) {
            v(tag, "", e);
        }
    }

    public static void v(String tag, String format, Object arg1) {
        if (DEBUG) {
            android.util.Log.v(getFinalTag(tag), getFinalLog(logFormat(format, arg1)));
        }
    }

    public static void v(String tag, String format, Object arg1, Object arg2) {
        if (DEBUG) {
            android.util.Log.v(getFinalTag(tag), getFinalLog(logFormat(format, arg1, arg2)));
        }
    }

    public static void v(String tag, String format, Object arg1, Object arg2, Object arg3) {
        if (DEBUG) {
            android.util.Log.v(getFinalTag(tag), getFinalLog(logFormat(format, arg1, arg2, arg3)));
        }
    }

    public static void v(String tag, String format, Object... args) {
        if (DEBUG) {
            android.util.Log.v(getFinalTag(tag), getFinalLog(logFormat(format, args)));
        }
    }

    public static void i(String tag, String msg) {
        if (DEBUG) {
            android.util.Log.i(getFinalTag(tag), getFinalLog(msg));
        }
    }

    public static void i(String tag, Throwable e) {
        if (DEBUG) {
            i(tag, "", e);
        }
    }

    public static void i(String tag, String format, Object arg1) {
        if (DEBUG) {
            android.util.Log.i(getFinalTag(tag), getFinalLog(logFormat(format, arg1)));
        }
    }

    public static void i(String tag, String format, Object arg1, Object arg2) {
        if (DEBUG) {
            android.util.Log.i(getFinalTag(tag), getFinalLog(logFormat(format, arg1, arg2)));
        }
    }

    public static void i(String tag, String format, Object arg1, Object arg2, Object arg3) {
        if (DEBUG) {
            android.util.Log.i(getFinalTag(tag), getFinalLog(logFormat(format, arg1, arg2, arg3)));
        }
    }

    public static void i(String tag, String format, Object... args) {
        if (DEBUG) {
            android.util.Log.i(getFinalTag(tag), getFinalLog(logFormat(format, args)));
        }
    }

    private static String getFinalLog(String msg) {
        StringBuilder sb = new StringBuilder("[").append(sDateFormat.format(new Date()))
                .append("]")
                .append(" ").append(msg);
        if (WITH_FILE_INFO) {
            sb.append("  ").append(getFileLineInfo());
        }
        return sb.toString();
    }

    private static String getFileLineInfo() {
        StringBuilder sb = new StringBuilder();
        Thread t = Thread.currentThread();
        StackTraceElement[] st = t.getStackTrace();
        int i = 0;
        while (i < st.length && !st[i].getClassName().equals(VLog.class.getName())) {
            ++i;
        }
        while (i < st.length && st[i].getClassName().equals(VLog.class.getName())) {
            ++i;
        }
        if (i < st.length) {
            sb.append("[").append(st[i].getFileName()).append("(").append(st[i].getLineNumber())
                    .append(")]");
        }
        sb.append("[").append(t.getId()).append("]");

        return sb.toString();
    }

    private static String logFormat(String format, Object... args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof String[]) {
                args[i] = prettyArray((String[]) args[i]);
            }
        }
        String msg = "";
        try {
            msg = String.format(format, args);
        } catch (Exception e) {
            StringBuilder sb = new StringBuilder("log error: the format is \"").append(format)
                    .append("\", the args is: ").append(Arrays.toString(args));
            android.util.Log.d(TAG, sb.toString());
            e.printStackTrace();
        }
        StringBuilder sb = new StringBuilder(msg);
        if (args.length > 0 && args[args.length - 1] instanceof Throwable) {
            Throwable tr = (Throwable) args[args.length - 1];
            sb.append(android.util.Log.getStackTraceString(tr));
            Throwable subTr = tr.getCause();
            if (subTr != null) {
                sb.append("\ncaused by:\n").append(android.util.Log.getStackTraceString(subTr));
            }
        }
        return sb.toString();
    }

    private static String prettyArray(String[] array) {
        if (array.length == 0) {
            return "[]";
        }

        StringBuilder sb = new StringBuilder("[");
        int len = array.length - 1;
        for (int i = 0; i < len; i++) {
            sb.append(array[i]);
            sb.append(", ");
        }
        sb.append(array[len]);
        sb.append("]");

        return sb.toString();
    }

    private static String getFinalTag(String tag) {
        return new StringBuilder(TAG).append("_").append(tag).toString();
    }
}
