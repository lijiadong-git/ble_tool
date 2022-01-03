package com.zxw.ui;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.lang.reflect.Constructor;

public class BaseFragment extends Fragment {

    private static final String ARG_SENDER = "ARG_SENDER";

    protected ICmdSender cmdSender;

    public static <T extends BaseFragment> T newInstance(Class<T> tClass, ICmdSender sender) {
        T fragment = null;
        try {
            fragment = tClass.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Bundle args = new Bundle();
        args.putSerializable(ARG_SENDER, sender);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle arguments = getArguments();
        if (arguments != null) {
            cmdSender = (ICmdSender) arguments.getSerializable(ARG_SENDER);
        }
    }
}
