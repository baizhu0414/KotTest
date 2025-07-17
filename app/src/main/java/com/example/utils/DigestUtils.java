package com.example.utils;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;

public class DigestUtils {

    private static final char[] HEX_CHARS = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'a', 'b', 'c', 'd', 'e', 'f'
    };

    /**
     * get hex string of specified bytes
     */
    public static String toHexString(byte[] bytes) {
        if (bytes == null) {
            throw new NullPointerException("bytes is null");
        }
        return toHexString(bytes, 0, bytes.length);
    }

    /**
     * get hex string of specified bytes
     */
    public static String toHexString(byte[] bytes, int off, int len) {
        if (bytes == null) {
            throw new NullPointerException("bytes is null");
        }
        if (off < 0 || (off + len) > bytes.length) {
            throw new IndexOutOfBoundsException();
        }
        char[] buff = new char[len * 2];
        int v;
        int c = 0;
        for (int i = 0; i < len; i++) {
            v = bytes[i + off] & 0xff;
            buff[c++] = HEX_CHARS[(v >> 4)];
            buff[c++] = HEX_CHARS[(v & 0x0f)];
        }
        return new String(buff, 0, len * 2);
    }

    public static byte[] hexStringToBytes(final String s) throws IllegalArgumentException {
        if (s == null || (s.length() % 2) == 1) {
            throw new IllegalArgumentException("hexBinary needs to be even-length: " + s);
        }
        final char[] chars = s.toCharArray();
        final int len = chars.length;
        final byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(chars[i], 16) << 4) + Character.digit(chars[i + 1], 16));
        }
        return data;
    }

    /**
     * get hexadecimal md5 digest of file
     */
    public static String md5Hex(File file) {
        try {
            MessageDigest digester = MessageDigest.getInstance("MD5");
            if (digester == null) {
                return null;
            }
            FileInputStream in = new FileInputStream(file);
            byte[] buff = new byte[1024 * 8];
            int n;
            while ((n = in.read(buff, 0, buff.length)) > 0) {
                digester.update(buff, 0, n);
            }
            in.close();
            return toHexString(digester.digest());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * get hexadecimal md5 digest of given string (its UTF-8 encoded bytes)
     */
    public static String md5Hex(String str) {
        try {
            if (str == null || str.length() == 0) {
                return null;
            }
            MessageDigest digester = MessageDigest.getInstance("MD5");
            byte[] data = str.getBytes("UTF-8");
            digester.update(data);
            return toHexString(digester.digest());
        } catch (Exception e) {
            return null;
        }
    }

    public static String md5Hex(byte[] data) {
        try {
            if (data == null || data.length == 0) {
                return null;
            }
            MessageDigest digester = MessageDigest.getInstance("MD5");
            digester.update(data);
            return toHexString(digester.digest());
        } catch (Exception e) {
            return null;
        }
    }

    public static String md5Hex(byte[] data, int off, int len) {
        try {
            if (data == null || off < 0 || len <= 0 || off + len > data.length) {
                return null;
            }
            MessageDigest digester = MessageDigest.getInstance("MD5");
            digester.update(data, off, len);
            return toHexString(digester.digest());
        } catch (Exception e) {
            return null;
        }
    }

}
