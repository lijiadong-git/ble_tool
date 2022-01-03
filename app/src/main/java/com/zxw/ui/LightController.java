package com.zxw.ui;

import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;

import com.sevenheaven.segmentcontrol.SegmentControl;
import com.zxw.R;

public class LightController {

    private static final String TAG = LightController.class.getSimpleName();

    public static final int[] SEGMENTS = new int[] {
            50, 60, 70, 80, 90, 100
    };

    private final Spinner modeSpinner;
    private final View dayView;
    private final View nightView;
    private final SegmentControl daySegment;
    private final SegmentControl nightSegment;

    private int dayLight = SEGMENTS[0];
    private int nightLight = SEGMENTS[1];

    public LightController(View rootView) {
        modeSpinner = rootView.findViewById(R.id.mode_checker);
        dayView = rootView.findViewById(R.id.light_config_layout_day);
        nightView = rootView.findViewById(R.id.light_config_layout_night);
        daySegment = rootView.findViewById(R.id.segment_control_day);
        nightSegment = rootView.findViewById(R.id.segment_control_night);
        init();
    }

    private void init() {
        modeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "select light mode --- " + position);
                switch (position) {
                    case 0:
                        // 白天
                        dayView.setVisibility(View.VISIBLE);
                        nightView.setVisibility(View.GONE);
                        break;

                    case 1:
                        // 夜晚
                        dayView.setVisibility(View.GONE);
                        nightView.setVisibility(View.VISIBLE);
                        break;

                    default:
                        dayView.setVisibility(View.VISIBLE);
                        nightView.setVisibility(View.VISIBLE);
                        break;
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                dayView.setVisibility(View.GONE);
                nightView.setVisibility(View.GONE);
            }
        });
        modeSpinner.setSelection(0);

        //
        daySegment.setOnSegmentControlClickListener(index -> {
            Log.d(TAG, "select day light --- " + SEGMENTS[index]);
            dayLight = SEGMENTS[index];
        });

        nightSegment.setOnSegmentControlClickListener(index -> {
            Log.d(TAG, "select night light --- " + SEGMENTS[index]);
            nightLight = SEGMENTS[index];
        });
    }

    public int getMode() {
        return modeSpinner.getSelectedItemPosition();
    }

    public void setMode(int m) {
        if (m < 0 || m >= 3) {
            return;
        }
        modeSpinner.setSelection(m);
    }

    public int getDayLight() {
        return dayLight;
    }

    public void setDayLight(int light) {
        for (int i = 0; i < SEGMENTS.length; i ++) {
            if (SEGMENTS[i] == light) {
                daySegment.setSelectedIndex(i);
                dayLight = light;
                return;
            }
        }
    }

    public int getNightLight() {
        return nightLight;
    }

    public void setNightLight(int light) {
        for (int i = 0; i < SEGMENTS.length; i ++) {
            if (SEGMENTS[i] == light) {
                nightSegment.setSelectedIndex(i);
                nightLight = light;
                return;
            }
        }
    }
}
