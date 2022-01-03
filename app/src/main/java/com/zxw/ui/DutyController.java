package com.zxw.ui;

import android.util.Log;
import android.view.View;

import androidx.annotation.IntDef;

import com.sevenheaven.segmentcontrol.SegmentControl;
import com.zxw.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class DutyController {

    private static final String TAG = DutyController.class.getSimpleName();

    public static final int DUTY_05_05 = 0;
    public static final int DUTY_25_75 = 1;
    public static final int DUTY_10_05 = 2;
    public static final int DUTY_10_10 = 3;
    public static final int DUTY_ALWAYS = 4;

    @IntDef({DUTY_05_05, DUTY_25_75, DUTY_10_05, DUTY_10_10, DUTY_ALWAYS})
    @Retention(RetentionPolicy.SOURCE)
    public @interface DUTY_VALUES {}


    private final  SegmentControl ch1Segment;
    private final  SegmentControl ch2Segment;
    private final  SegmentControl ch3Segment;

    private int ch1Duty = DUTY_05_05;
    private int ch2Duty = DUTY_05_05;
    private int ch3Duty = DUTY_05_05;

    public DutyController(View rootView) {
        ch1Segment = rootView.findViewById(R.id.segment_control_ch1);
        ch2Segment = rootView.findViewById(R.id.segment_control_ch2);
        ch3Segment = rootView.findViewById(R.id.segment_control_ch3);

        initViews();
    }

    private void initViews() {
        ch1Segment.setOnSegmentControlClickListener(index -> {
            Log.d(TAG, "select ch1 duty --- " + index);
            ch1Duty = index;
        });

        ch2Segment.setOnSegmentControlClickListener(index -> {
            Log.d(TAG, "select ch2 duty --- " + index);
            ch2Duty = index;
        });

        ch3Segment.setOnSegmentControlClickListener(index -> {
            Log.d(TAG, "select ch3 duty --- " + index);
            ch3Duty = index;
        });
    }

    public @DUTY_VALUES int getCh1Duty() {
        return ch1Duty;
    }

    public @DUTY_VALUES int getCh2Duty() {
        return ch2Duty;
    }

    public @DUTY_VALUES int getCh3Duty() {
        return ch3Duty;
    }

    public void setCh1Duty(@DUTY_VALUES int value) {
        ch1Segment.setSelectedIndex(value);
        ch1Duty = value;
    }

    public void setCh2Duty(@DUTY_VALUES int value) {
        ch2Segment.setSelectedIndex(value);
        ch2Duty = value;
    }

    public void setCh3Duty(@DUTY_VALUES int value) {
        ch3Segment.setSelectedIndex(value);
        ch3Duty = value;
    }
}
