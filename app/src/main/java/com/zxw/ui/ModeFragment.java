package com.zxw.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.zxw.R;

public class ModeFragment extends BaseFragment implements View.OnClickListener {

    private static final String TAG = ModeFragment.class.getSimpleName();

    public ModeFragment() {
        // Required empty constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mode, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        findViews();
    }

    private void findViews() {

        View view = getView();
        if (null == view) {
            Log.e(TAG, "Error find views");
            return;
        }

        View modeStdView = view.findViewById(R.id.mode_std);
        View modeSynView = view.findViewById(R.id.mode_syn);
        View modeFlowView = view.findViewById(R.id.mode_flow);
        View modeRrfbView = view.findViewById(R.id.mode_rrfb);
        View modeIotView = view.findViewById(R.id.mode_iot);

        modeStdView.setOnClickListener(this);
        modeSynView.setOnClickListener(this);
        modeFlowView.setOnClickListener(this);
        modeRrfbView.setOnClickListener(this);
        modeIotView.setOnClickListener(this);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {

        switch (v.getId()) {

            case R.id.mode_std: {
                Fragment fragment = BaseFragment.newInstance(StdFragment.class, cmdSender);
                FragmentManager fragmentManager = getFragmentManager();
                FragmentTransaction transaction = fragmentManager.beginTransaction();
                transaction.replace(R.id.fragment_content, fragment);
                transaction.addToBackStack("Std");
                transaction.commit();
                break;
            }

            case R.id.mode_syn:
                break;

            case R.id.mode_flow:
                break;

            case R.id.mode_rrfb:
                break;

            case R.id.mode_iot:
                break;

            default:
                Log.e(TAG, "no one handle this --- " + v.getTag());
                break;
        }
    }
}