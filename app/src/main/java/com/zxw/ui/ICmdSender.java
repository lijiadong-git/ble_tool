package com.zxw.ui;

import com.zxw.cmd.CmdDefine;
import com.zxw.cmd.CmdEntry;

import java.io.Serializable;

public interface ICmdSender extends Serializable {
    void send(CmdEntry entry, CmdDefine.ICmdCallback callback);
}
