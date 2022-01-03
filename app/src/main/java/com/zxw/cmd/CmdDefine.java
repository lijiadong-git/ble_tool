package com.zxw.cmd;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class CmdDefine {

    // -----------------------------------------------------------
    public static final int CMD_GET_LIGHT = 0;
    public static final int CMD_SET_LIGHT = 1;
    public static final int CMD_GET_DUTY = 2;
    public static final int CMD_SET_DUTY = 3;
    @IntDef({
            CMD_GET_LIGHT,
            CMD_SET_LIGHT,
            CMD_GET_DUTY,
            CMD_SET_DUTY
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface CMD {
    }


    // -----------------------------------------------------------
    public static final int CMD_RESULT_OK = 0;
    public static final int CMD_RESULT_FAILED = 1;
    public static final int CMD_RESULT_TIMEOUT = 2;
    @IntDef({
            CMD_RESULT_OK,
            CMD_RESULT_FAILED,
            CMD_RESULT_TIMEOUT
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface CMD_RESULT {
    }


    // -----------------------------------------------------------
    public interface ICmdCallback {
        void onResult(@CMD_RESULT int result, CmdEntry entryRes);
    }
}
