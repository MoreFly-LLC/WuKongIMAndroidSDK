package com.xinbida.wukongim.log;

public class WKLog implements IWKLog {

    private static class WKLogBinder {
        private final static WKLog INSTANCE = new WKLog();
    }

    public static WKLog getInstance() {
        return WKLog.WKLogBinder.INSTANCE;
    }

    private IWKLog realPrinter = null;

    void setCustomLogPrinter(IWKLog printer) {
        realPrinter = printer;
    }

    @Override
    public void d(String tag, String msg) {
        if (realPrinter != null) {
            realPrinter.d(tag, msg);
        }
    }

    @Override
    public void i(String tag, String msg) {
        if (realPrinter != null) {
            realPrinter.i(tag, msg);
        }
    }

    @Override
    public void w(String tag, String msg) {
        if (realPrinter != null) {
            realPrinter.w(tag, msg);
        }
    }

    @Override
    public void e(String tag, String msg) {
        if (realPrinter != null) {
            realPrinter.e(tag, msg);
        }
    }
}
