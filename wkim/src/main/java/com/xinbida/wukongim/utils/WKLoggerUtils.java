package com.xinbida.wukongim.utils;

import android.annotation.SuppressLint;
import android.util.Log;

import com.xinbida.wukongim.WKIM;
import com.xinbida.wukongim.WKIMApplication;
import com.xinbida.wukongim.log.WKLog;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

/**
 * 2019-11-10 17:22
 * 日志管理
 */
public class WKLoggerUtils {

    /**
     * log TAG
     */
    private final String TAG = "WKLogger" + WKIM.getInstance().getVersion();
    private final String FILE_NAME = "wkLogger_" + WKIM.getInstance().getVersion() + ".log";

    private String getLogFilePath() {
        final String ROOT = Objects.requireNonNull(WKIMApplication.getInstance().getContext().getExternalFilesDir(null)).getAbsolutePath() + "/";
        return ROOT + FILE_NAME;
    }

    private WKLoggerUtils() {

    }

    private static class LoggerUtilsBinder {
        private final static WKLoggerUtils utils = new WKLoggerUtils();
    }

    public static WKLoggerUtils getInstance() {
        return LoggerUtilsBinder.utils;
    }

    /**
     * 获取函数名称
     */
    private String getFunctionName() {
        StackTraceElement[] sts = Thread.currentThread().getStackTrace();

        for (StackTraceElement st : sts) {
            if (st.isNativeMethod()) {
                continue;
            }

            if (st.getClassName().equals(Thread.class.getName())) {
                continue;
            }

            if (st.getClassName().equals(this.getClass().getName())) {
                continue;
            }

            return "[" + Thread.currentThread().getName() + "(" + Thread.currentThread().getId()
                    + "): " + st.getFileName() + ":" + st.getLineNumber() + "]";
        }

        return null;
    }

    private String createMessage(String msg) {
        String functionName = getFunctionName();
        return (functionName == null ? msg : (functionName + " - " + msg));
    }

    /**
     * log.i
     */
    private void info(String tag, String msg) {
        String message = createMessage(msg);
        Log.i(TAG + " " + tag, message);
        writeLog(message);
    }

    public void i(String msg) {
        if (WKIM.getInstance().isDebug()) {
            info(TAG, msg);
        }
        WKLog.getInstance().i(TAG, msg);
    }

    public void i(String tag, String msg) {
        if (WKIM.getInstance().isDebug()) {
            info(tag, msg);
        }
        WKLog.getInstance().i(TAG + " " + tag, msg);
    }

    /**
     * log.warn
     */
    private void warn(String tag, String msg) {
        String message = createMessage(msg);
        Log.w(TAG + " " + tag, message);
        writeLog(message);
    }

    /**
     * log.w
     */
    public void w(String tag, String msg) {
        if (WKIM.getInstance().isDebug()) {
            warn(tag, msg);
        }
        WKLog.getInstance().w(TAG + " " + tag, msg);
    }

    /**
     * log.e
     */
    private void error(String tag, String msg) {
        String message = createMessage(msg);
        Log.e(TAG + " " + tag, message);
        writeLog(message);
    }

    public void e(String msg) {
        if (WKIM.getInstance().isDebug()) {
            error("", msg);
        }
        WKLog.getInstance().e(TAG, msg);
    }

    public void e(String tag, String msg) {
        if (WKIM.getInstance().isDebug()) {
            error(tag, msg);
        }
        WKLog.getInstance().e(TAG + " " + tag, msg);
    }

    @SuppressLint("SimpleDateFormat")
    private void writeLog(String content) {
        try {
            if (WKIMApplication.getInstance().getContext() == null || !WKIM.getInstance().isWriteLog()) {
                return;
            }
            File file = new File(getLogFilePath());
            if (!file.exists()) {
                file.createNewFile();
            }
            SimpleDateFormat formate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            FileWriter write = new FileWriter(file, true);
            write.write(formate.format(new Date()) + "   " + content + "\n");
            write.flush();
            write.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
