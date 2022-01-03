package com.zxw.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.zxw.R;
import com.zxw.cmd.CmdDefine;
import com.zxw.cmd.CmdEntry;

import java.util.Arrays;

import static com.zxw.cmd.CmdDefine.CMD_RESULT_TIMEOUT;

public class StdFragment extends BaseFragment implements View.OnClickListener {

    private static final String TAG = StdFragment.class.getSimpleName();

    private LightController lightController;
    private DutyController dutyController;

    public StdFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_std, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initViews();
    }

    private void initViews() {

        View view = getView();
        if (null == view) {
            Log.e(TAG, "Error find views");
            return;
        }

        View saveButton = view.findViewById(R.id.btn_save);
        saveButton.setOnClickListener(this);

        View resetButton = view.findViewById(R.id.btn_reset);
        resetButton.setOnClickListener(this);

        View lightConfigView = view.findViewById(R.id.light_config_layout);
        lightController = new LightController(lightConfigView);

        lightController.setMode(3);
        lightController.setDayLight(70);
        lightController.setNightLight(60);

        View dutyView = view.findViewById(R.id.duty_config_layout);
        dutyController = new DutyController(dutyView);

        dutyController.setCh1Duty(DutyController.DUTY_10_05);
        dutyController.setCh2Duty(DutyController.DUTY_10_10);
        dutyController.setCh3Duty(DutyController.DUTY_ALWAYS);
        dutyController.setCh3Duty(DutyController.DUTY_25_75);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.btn_save: {
                byte cmd = (byte) CmdDefine.CMD_SET_LIGHT;
                byte mode = (byte) lightController.getMode();
                byte dayLight = (byte) lightController.getDayLight();
                byte nightLight = (byte) lightController.getNightLight();
                byte[] data = new byte[] {
                        mode, dayLight, nightLight
                };
                CmdEntry entrySnd = new CmdEntry(cmd, data);
                if (null != cmdSender) {
                    cmdSender.send(entrySnd, (result, entryRes) -> {
                        if (result == CMD_RESULT_TIMEOUT) {
                            return;
                        }
                        Log.d(TAG, "get result --- " + result);
                        if (entryRes != null) {
                            Log.d(TAG, "get cmd --- " + entryRes.getCmd());
                            Log.d(TAG, "get data --- " + new String(entryRes.getDataBytes()));
                        }
                    });
                }
                break;
            }

            case R.id.btn_reset: {
                break;
            }

            default:
                break;
        }
    }
}