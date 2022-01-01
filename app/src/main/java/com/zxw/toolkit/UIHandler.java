package com.zxw.toolkit;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

public class UIHandler {

    private static final Handler mHandler = new Handler(Looper.getMainLooper());

    public static boolean post(@NonNull Runnable runnable) {
        return mHandler.post(runnable);
    }

    public static boolean postDelayed(@NonNull Runnable r, long delay) {
        return mHandler.postDelayed(r, delay);
    }
}
