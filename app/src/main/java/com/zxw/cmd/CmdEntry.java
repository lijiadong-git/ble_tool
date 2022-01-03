package com.zxw.cmd;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class CmdEntry implements Parcelable {
    private static final String TAG = CmdEntry.class.getSimpleName();
    private final byte cmd;
    private final byte[] bytes;

    public CmdEntry(byte cmd, byte[] bytes) {
        this.cmd = cmd;
        this.bytes = bytes;
    }

    protected CmdEntry(Parcel in) {
        cmd = in.readByte();
        bytes = in.createByteArray();
    }

    public static final Creator<CmdEntry> CREATOR = new Creator<CmdEntry>() {
        @Override
        public CmdEntry createFromParcel(Parcel in) {
            return new CmdEntry(in);
        }

        @Override
        public CmdEntry[] newArray(int size) {
            return new CmdEntry[size];
        }
    };

    public byte getCmd() {
        return this.cmd;
    }

    public byte[] getDataBytes() {
        return this.bytes;
    }

    public byte[] getCmdBytes() {
        int dataLen = this.bytes.length;
        if (dataLen > 255) {
            Log.e(TAG, " send data len is too large --- " + dataLen);
            return null;
        }
        int sendLen = dataLen + 6;
        byte[] sendBytes = new byte[sendLen];
        sendBytes[0] = 'Y';
        sendBytes[1] = 'H';
        sendBytes[2] = 'Z';
        sendBytes[3] = 'X';
        sendBytes[4] = this.cmd;
        sendBytes[5] = (byte) dataLen;
        System.arraycopy(this.bytes, 0, sendBytes, 6, dataLen);
        return sendBytes;
    }

    @Override
    public int describeContents() {
        return cmd;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte(cmd);
        dest.writeByteArray(bytes);
    }
}
