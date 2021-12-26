package com.zxw.util;

public class Util {
    /**
     * 将16进制字符串转换为byte[]
     */
    public static byte[] hexString2ByteArray(String bs) {
        if (bs == null) {
            return null;
        }
        int bsLength = bs.length();
        if (bsLength % 2 != 0) {
            bs = "0" + bs;
            bsLength = bs.length();
        }
        byte[] cs = new byte[bsLength / 2];
        String st;
        for (int i = 0; i < bsLength; i = i + 2) {
            st = bs.substring(i, i + 2);
            cs[i / 2] = (byte) Integer.parseInt(st, 16);
        }
        return cs;
    }

    /**
     * byte数组转String
     */
    public static String bytesToHexString(byte[] bArray) {
        StringBuilder sb = new StringBuilder(bArray.length);
        String sTemp;
        for (int i = 0; i < bArray.length; i++) {
            sTemp = Integer.toHexString(0xFF & bArray[i]);
            if (sTemp.length() < 2) {
                sb.append(0);
            }
            sb.append(sTemp.toUpperCase());
        }
        int length = sb.length();
        if (length == 1 || length == 0) {
            return sb.toString();
        }
        if (length % 2 == 1) {
            sb.insert(length - 1, " ");
            length = length - 1;
        }
        for (int i = length; i > 0; i = i - 2) {
            sb.insert(i, " ");
        }
        return sb.toString();
    }
}
