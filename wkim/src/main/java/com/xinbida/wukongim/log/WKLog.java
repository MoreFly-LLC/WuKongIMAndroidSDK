package com.xinbida.wukongim.log;

import androidx.annotation.NonNull;

public class WKLog implements IWKLog {

    private static class WKLogBinder {
        private final static WKLog INSTANCE = new WKLog();
    }

    public static WKLog getInstance() {
        return WKLog.WKLogBinder.INSTANCE;
    }

    private IWKLog realPrinter = null;

    public void setCustomLogPrinter(IWKLog printer) {
        realPrinter = printer;
    }

    @Override
    public void d(@NonNull String tag, @NonNull String msg) {
        if (realPrinter != null) {
            realPrinter.d(tag, msg);
        }
    }

    @Override
    public void i(@NonNull String tag, @NonNull String msg) {
        if (realPrinter != null) {
            realPrinter.i(tag, msg);
        }
    }

    @Override
    public void w(@NonNull String tag, @NonNull String msg) {
        if (realPrinter != null) {
            realPrinter.w(tag, msg);
        }
    }

    @Override
    public void e(@NonNull String tag, @NonNull String msg) {
        if (realPrinter != null) {
            realPrinter.e(tag, msg);
        }
    }
}
