package com.xinbida.wukongim.message.timer;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import com.xinbida.wukongim.WKIM;
import com.xinbida.wukongim.message.WKConnection;
import com.xinbida.wukongim.protocol.WKPingMsg;
import com.xinbida.wukongim.utils.WKLoggerUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class HeartbeatManager {
    private static final String TAG = "HeartbeatManager";
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final ReentrantLock heartbeatLock = new ReentrantLock();
    private final List<PingInfo> heartBeatQueue;
    private static final int PING_PERIOD = 15000;
    private static final int SERVER_TIME_OUT = 180000;
    private final long mFirstPingTimeout;
    private HeartBeatListener heartBeatListener;
    private volatile boolean isBackground;
    private volatile boolean firstForegroundPingCheck;
    private volatile long lastPongOrConnectedTime;
    private final int mPingTimeoutNumbers;

    private HeartbeatManager() {
        this.heartBeatQueue = Collections.synchronizedList(new LinkedList<>());
        this.mFirstPingTimeout = 2000L;
        this.mPingTimeoutNumbers = 3;
    }

    public static HeartbeatManager getInstance() {
        return HeartbeatManager.HeartBeatManagerHolder.instance;
    }

    public void setHeartBeatListener(HeartBeatListener heartBeatListener) {
        this.heartBeatListener = heartBeatListener;
    }

    public void startHeartbeat() {
        WKLoggerUtils.getInstance().i(TAG, "startHeartbeat");
        TimerManager.getInstance().addTask(
                TimerTasks.HEARTBEAT,
                () -> {
                    heartbeatLock.lock();
                    try {
                        sendPing();
                    } finally {
                        heartbeatLock.unlock();
                    }
                },
                0,
                PING_PERIOD
        );
    }

    private void stopHeartbeat() {
        WKLoggerUtils.getInstance().i(TAG, "stopHeartbeat");
        TimerManager.getInstance().removeTask(TimerTasks.HEARTBEAT);
    }

    void sendPing() {
        WKLoggerUtils.getInstance().i(TAG, "sendPing");
        int responseCode = WKConnection.getInstance().sendMessage(new WKPingMsg());
        this.enqueue(SystemClock.elapsedRealtime(), responseCode, false);
    }

    public void onReceivePong() {
        long curTime = SystemClock.elapsedRealtime();
        this.lastPongOrConnectedTime = curTime;
        boolean isTimeOut = this.isPingTimeOut(curTime);
        WKLoggerUtils.getInstance().i(TAG, "onReceivePong，isTimeOut = " + isTimeOut);
        if (isTimeOut) {
            this.resetQueueAndReconnect(PingFailedReason.RECEIVE_PONG_TIMEOUT);
        } else if (!heartBeatQueue.isEmpty()) {
            heartBeatQueue.remove(0);
        }
    }

    public void onConnectionStatusChange(final boolean isConnected) {
        if (isConnected) {
            lastPongOrConnectedTime = SystemClock.elapsedRealtime();
            if (this.isBackground) {
                this.stopHeartbeat();
            } else {
                this.heartBeatQueue.clear();
                this.startHeartbeat();
            }
        } else {
            this.heartBeatQueue.clear();
            this.stopHeartbeat();
        }
    }

    public void onAppBackgroundChanged(final boolean isBackground) {
        this.isBackground = isBackground;
        boolean isConnected = WKIM.getInstance().getConnectionManager().isConnected();
        WKLoggerUtils.getInstance().i(TAG, "onAppBackgroundChanged = " + isBackground + ",isConnected=" + isConnected);
        if (isBackground) {
            if (!isConnected) {
                this.heartBeatQueue.clear();
            }
            this.stopHeartbeat();
        } else {
            this.onForeground(isConnected);
        }
    }

    private void onForeground(final boolean isConnected) {
        if (isConnected) {
            long diff = SystemClock.elapsedRealtime() - this.lastPongOrConnectedTime;
            if (diff >= SERVER_TIME_OUT) {
                this.resetQueueAndReconnect(PingFailedReason.SERVER_TIMEOUT);
                return;
            }
            this.heartBeatQueue.clear();
            this.sendFirstForegroundPing();
        } else {
            this.heartBeatQueue.clear();
            this.stopHeartbeat();
            this.resetQueueAndReconnect(PingFailedReason.NO_CONNECTED_FIRST_FOREGROUND);
        }
    }

    private void sendFirstForegroundPing() {
        WKLoggerUtils.getInstance().i(TAG, "interval|isFirst");
        firstForegroundPingCheck = true;
        sendPing();
        mHandler.postDelayed(new Runnable() {
            public void run() {
                if (isFirstPingTimeout()) {
                    resetQueueAndReconnect(PingFailedReason.FIRST_PING_TIMEOUT);
                    WKLoggerUtils.getInstance().i(TAG, "timer check time = " + SystemClock.elapsedRealtime() + ",isTimeOut = " + true);
                } else {
                    WKLoggerUtils.getInstance().i(TAG, "timer check time = " + SystemClock.elapsedRealtime() + ",isTimeOut = " + false);
                    if (!isBackground) {
                        startHeartbeat();
                    } else {
                        WKLoggerUtils.getInstance().i(TAG, "app is background ,needn't start");
                    }
                }
            }
        }, mFirstPingTimeout);
    }

    private boolean isFirstPingTimeout() {
        List<PingInfo> queue = new ArrayList<>(this.heartBeatQueue);
        if (queue.size() == 2) {
            return true;
        } else if (queue.size() != 1) {
            return false;
        } else {
            PingInfo pingInfo = this.getPingInfo(0);
            return pingInfo != null && !pingInfo.isBackgroundPing();
        }
    }

    private void enqueue(long ts, int pingResponseCode, boolean isBackground) {
        boolean isTimeOut = this.isPingTimeOut(ts);
        WKLoggerUtils.getInstance().i(TAG, "enqueue pingCode =" + pingResponseCode + ",time=" + ts + ",isTimeOut = " + isTimeOut);
        if (isTimeOut) {
            this.resetQueueAndReconnect(PingFailedReason.LOST_PONG_LIMIT);
        } else if (pingResponseCode != 0) {
            this.heartBeatQueue.add(new PingInfo(ts, isBackground));
        }
    }

    private void resetQueueAndReconnect(PingFailedReason reason) {
        WKLoggerUtils.getInstance().i(TAG, "resetQueueAndReconnect:" + reason);
        this.heartBeatQueue.clear();
        if (this.heartBeatListener != null) {
            this.heartBeatListener.onPongReceiveFail(reason);
        }
    }

    static int getPingOutTime() {
        return PING_PERIOD * getInstance().mPingTimeoutNumbers;
    }

    private boolean isPingTimeOut(long time) {
        WKLoggerUtils.getInstance().i(TAG, "isPingTimeOut,heartBeatQueue size= " + this.heartBeatQueue.size());
        List<PingInfo> heartBeatQueue = this.heartBeatQueue;
        if (heartBeatQueue.isEmpty()) {
            return false;
        } else {
            PingInfo pingInfo = this.getPingInfo(0);
            if (pingInfo == null) {
                return false;
            } else {
                long firstTs = pingInfo.getTimestamp();
                if (this.firstForegroundPingCheck) {
                    this.firstForegroundPingCheck = false;
                    boolean result = time - firstTs > this.mFirstPingTimeout;
                    if (result) {
                        WKLoggerUtils.getInstance().i(TAG, "time|firstTs|timeOut");
                    }

                    return result;
                } else {
                    long tap = time - firstTs;
                    boolean isTimeOut = tap > (long) (getPingOutTime() - 1000);
                    if (isTimeOut) {
                        WKLoggerUtils.getInstance().i(TAG, "time|firstTs|tap");
                    }

                    return isTimeOut;
                }
            }
        }
    }

    private PingInfo getPingInfo(int index) {
        if (!this.heartBeatQueue.isEmpty() && index >= 0 && index < this.heartBeatQueue.size()) {
            PingInfo pingInfo = null;

            try {
                pingInfo = (PingInfo) this.heartBeatQueue.get(index);
            } catch (Exception e) {
                WKLoggerUtils.getInstance().i(TAG, "getPingInfo e " + e);
            }

            return pingInfo;
        } else {
            return null;
        }
    }

    static class HeartBeatManagerHolder {
        static final HeartbeatManager instance = new HeartbeatManager();

        HeartBeatManagerHolder() {
        }
    }

    private static class PingInfo {
        private final long timestamp;
        private final boolean isBackgroundPing;

        public PingInfo(long timestamp, boolean isBackgroundPing) {
            this.timestamp = timestamp;
            this.isBackgroundPing = isBackgroundPing;
        }

        public long getTimestamp() {
            return this.timestamp;
        }

        public boolean isBackgroundPing() {
            return this.isBackgroundPing;
        }
    }

    public static enum PingFailedReason {
        LOST_PONG_LIMIT,
        SERVER_TIMEOUT,
        FIRST_PING_TIMEOUT,
        RECEIVE_PONG_TIMEOUT,
        NO_CONNECTED_FIRST_FOREGROUND;

        private PingFailedReason() {
        }
    }

    public interface HeartBeatListener {
        void onPongReceiveFail(PingFailedReason reason);
    }
}
