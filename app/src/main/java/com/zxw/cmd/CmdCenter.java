package com.zxw.cmd;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.zxw.cmd.CmdDefine.CMD;
import com.zxw.cmd.CmdDefine.ICmdCallback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class CmdCenter {

    private static final String TAG = CmdEntry.class.getSimpleName();
    private static final CmdCenter instance = new CmdCenter();

    private final ConcurrentHashMap<Integer, CmdEntry> entries = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, List<SubscribeEntry>> callbacks = new ConcurrentHashMap<>();
    private final Handler handler;


    private static class SubscribeEntry {
        public @CMD int cmd;
        public ICmdCallback callback;
        public int timeout;
    }

    private CmdCenter() {
        HandlerThread handlerThread = new HandlerThread("CmdCenter");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    public static CmdCenter getInstance() {
        return instance;
    }

    public CmdEntry getEntry(@CMD int cmd) {
        return entries.get(cmd);
    }

    public void subscribe(@CMD int cmd, ICmdCallback callback, int timeout) {
        List<SubscribeEntry> list = callbacks.get(cmd);
        if (null == list) {
            list = Collections.synchronizedList(new ArrayList<>());
            callbacks.put(cmd, list);
        }
        SubscribeEntry entry = new SubscribeEntry();
        entry.cmd = cmd;
        entry.callback = callback;
        entry.timeout = timeout;
        list.add(entry);

        if (timeout > 0) {
            List<SubscribeEntry> finalList = list;
            handler.postDelayed(() -> {
                Log.e(TAG, "cmd: " + cmd + " timeout !!!");
                if (finalList.contains(entry)) {
                    finalList.remove(entry);
                    callback.onResult(CmdDefine.CMD_RESULT_TIMEOUT, null);
                }
            }, timeout * 1000);
        }
    }

    public void publish(@CMD int cmd, CmdEntry entry) {
        entries.put(cmd, entry);
        List<SubscribeEntry> list = callbacks.get(cmd);
        if (list == null) {
            return;
        }
        for (SubscribeEntry se : list) {
            se.callback.onResult(CmdDefine.CMD_RESULT_OK, entry);
        }
        list.clear();
    }
}
