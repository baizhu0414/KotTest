package com.example.utils;

import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class StringUtils {
    public static final String EMPTY = "";

    // 防止被继承
    private StringUtils() {
    }

    public static boolean isEmpty(String s) {
        if (s == null || s.length() == 0) {
            return true;
        }
        return false;
    }

    public static boolean equal(String s1, String s2) {
        return (isEmpty(s1) && isEmpty(s2) || s1 != null && s1.equals(s2));
    }

    private static final int STRING_MAX_NUM = 255;

    public static String ellipsize(String userName, int maxCharCount) {
        if (userName == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (int i = 0; i < userName.length(); i++) {
            char c = userName.charAt(i);
            if (c > 0 && c <= STRING_MAX_NUM) {
                count += 1;
            } else {
                count += 2;
            }
            if (count > maxCharCount) {
                sb.append("...");
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static final int CHAR_MAX = 0x1F;

    /**
     * Escape string for EcmaScript/JavaScript.
     * see android.util.JsonWrite
     *
     * @param out         output
     * @param str         input string
     * @param addQuotMark add surrounding quotation marks or not
     */
    public static void escapeEcmaScript(StringBuilder out, String str,
                                        boolean addQuotMark) {
        if (addQuotMark) {
            out.append("\"");
        }

        for (int i = 0, length = str.length(); i < length; i++) {
            char c = str.charAt(i);

            /*
             * From RFC 4627, "All Unicode characters may be placed within the
             * quotation marks except for the characters that must be escaped:
             * quotation mark, reverse solidus, and the control characters
             * (U+0000 through U+001F)."
             *
             * We also escape '\u2028' and '\u2029', which JavaScript interprets
             * as newline characters. This prevents eval() from failing with a
             * syntax error.
             * http://code.google.com/p/google-gson/issues/detail?id=341
             */
            switch (c) {
                case '"':
                case '\\':
                    out.append('\\').append(c);
                    break;
                case '\t':
                    out.append("\\t");
                    break;
                case '\b':
                    out.append("\\b");
                    break;
                case '\n':
                    out.append("\\n");
                    break;
                case '\r':
                    out.append("\\r");
                    break;
                case '\f':
                    out.append("\\f");
                    break;
                case '\u2028':
                case '\u2029':
                    out.append(String.format("\\u%04x", (int) c));
                    break;
                default:
                    if (c <= CHAR_MAX) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                    break;
            }
        }
        if (addQuotMark) {
            out.append("\"");
        }
    }


    public static String removeBlank(String s) {
        if (isEmpty(s)) {
            return "";
        }
        StringBuffer r = new StringBuffer();
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) >= '0' && s.charAt(i) <= '9') {
                r.append(s.charAt(i));
            }
        }
        return r.toString();
    }

    public static HashMap<String, String> parseResponse(String response) {
        int startIndex = response.indexOf('{');
        int endIndex = response.lastIndexOf('}');

        String[] values = response.substring(startIndex + 1, endIndex).split(",");
        for (int i = 0; i < values.length; i++) {
            values[i] = values[i].trim();
        }

        HashMap<String, String> h = new HashMap<String, String>();
        for (int i = 0; i < values.length; i++) {
            String[] ss = values[i].split(":", 2);
            h.put(clean(ss[0]), clean(ss[1]));
        }

        return h;
    }

    public static String clean(String s) {
        int startIndex = s.indexOf('"');
        int endIndex = s.lastIndexOf('"');

        if (startIndex >= 0 && endIndex > startIndex) {
            return s.substring(startIndex + 1, endIndex).trim();
        }

        return s;
    }

    // input string format: yyyy-MM-dd
    // remove seconds and year
    public static String getShortDateTime(String s) {
        int begin = s.indexOf('-') + 1;
        int end = s.lastIndexOf(':');

        if (begin > 0 && end > 0) {
            return s.substring(begin, end);
        }

        return s;
    }

    private static final int CHAR_32 = 32;
    private static final int CHAR_127 = 127;
    private static final int CHAR_12288 = 12288;
    private static final int CHAR_65248 = 65248;
    private static final int CHAR_65280 = 65280;
    private static final int CHAR_65375 = 65375;

    // 半角转化为全角的方法
    public static String toSBC(String input) {
        // 半角转全角：
        char[] c = input.toCharArray();
        for (int i = 0; i < c.length; i++) {
            if (c[i] == CHAR_32) {
                c[i] = (char) CHAR_12288;
                continue;
            }
            if (c[i] < CHAR_127 && c[i] > CHAR_32) {
                c[i] = (char) (c[i] + CHAR_65248);
            }
        }
        return new String(c);
    }

    // 全角转化为半角的方法
    public static String toDBC(String input) {
        char[] c = input.toCharArray();
        for (int i = 0; i < c.length; i++) {
            if (!isChinese(c[i])) {
                continue;
            }

            if (c[i] == CHAR_12288) {
                c[i] = (char) CHAR_32;
                continue;
            }

            if (c[i] > CHAR_65280 && c[i] < CHAR_65375) {
                c[i] = (char) (c[i] - CHAR_65248);
            }
        }
        return new String(c);
    }

    @SuppressWarnings("checkstyle:BooleanExpressionComplexity")
    public static boolean isChinese(char c) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        if (ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || ub == Character.UnicodeBlock.GENERAL_PUNCTUATION
                || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS) {
            return true;
        }
        return false;
    }

    /**
     * 替换ad_click_track_url占位符
     *
     * @param adClickTrackUrl
     * @return
     */
    public static String handleAdClickTrackUrl(String adClickTrackUrl) {
        String result = adClickTrackUrl;
        if (!isEmpty(result)) {
            try {
                Random random = new Random();
                result = result.replace("[ss_random]", String.valueOf(random.nextLong()));
                result = result.replace("[ss_timestamp]", String.valueOf(System.currentTimeMillis()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    private static final long K = 1024;
    private static final long M = K * K;
    private static final long G = M * K;
    private static final long T = G * K;

    private static String format(final long value,
                                 final long divider,
                                 final String unit) {
        final double result =
                divider > 1 ? (double) value / (double) divider : (double) value;
        return new DecimalFormat("#.##").format(result) + " " + unit;
    }

    public static String bytesToHuman(final long value) {
        final long[] dividers = new long[]{T, G, M, K, 1};
        final String[] units = new String[]{"TB", "GB", "MB", "KB", "B"};
        if (value < 1) {
            return 0 + " " + units[units.length - 1];
        }
        String result = null;
        for (int i = 0; i < dividers.length; i++) {
            final long divider = dividers[i];
            if (value >= divider) {
                result = format(value, divider, units[i]);
                break;
            }
        }
        return result;
    }

    public static String mapToString(Map<String, String> map) {
        if (map == null) {
            return null;
        }
        try {
            JSONObject json = new JSONObject();
            for (String key : map.keySet()) {
                String value = map.get(key);
                if (StringUtils.isEmpty(key) || StringUtils.isEmpty(value)) {
                    continue;
                }
                json.put(key, value);
            }
            return json.toString();
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    public static Map<String, String> stringToMap(String input, Map<String, String> map) {
        if (StringUtils.isEmpty(input) || map == null) {
            return null;
        }
        try {
            JSONObject json = new JSONObject(input);
            Iterator<?> it = json.keys();
            while (it.hasNext()) {
                String key = (String) it.next();
                String value = json.getString(key);
                if (StringUtils.isEmpty(key)) {
                    continue;
                }
                if (StringUtils.isEmpty(value)) {
                    continue;
                }
                map.put(key, value);
            }
        } catch (Exception e) {
            // ignore
        }
        return map;
    }

    public static String strEncode(String string) {
        if (!isEmpty(string)) {
            try {
                return URLEncoder.encode(string, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                // ignore
            }
        }
        return string;
    }

    private static final int XOR_MASK = 0x5;

    public static String encryptWithXor(String normalCode) {
        try {
            if (TextUtils.isEmpty(normalCode)) {
                return null;
            }
            byte[] data = normalCode.getBytes("UTF-8");
            for (int i = 0; i < data.length; i++) {
                data[i] ^= (byte) XOR_MASK;
            }
            return DigestUtils.toHexString(data, 0, data.length);
        } catch (Exception e) {
            return normalCode;
        }
    }

    public static String decryptWithXor(String encryptCode, Boolean... isBase64) {
        try {
            if (TextUtils.isEmpty(encryptCode)) {
                return null;
            }
            byte[] data;
            if (isBase64 != null && isBase64.length > 0 && isBase64[0]) {
                data = Base64.decode(encryptCode, Base64.DEFAULT);
            } else {
                data = DigestUtils.hexStringToBytes(encryptCode);
            }
            for (int i = 0; i < data.length; i++) {
                data[i] ^= (byte) XOR_MASK;
            }
            return new String(data, 0, data.length, "UTF-8");
        } catch (Exception e) {
            return encryptCode;
        }
    }

    public static String trimString(String str) {
        if (str == null || str.length() == 0) {
            return str;
        }
        return str.trim();
    }

    public static String compressWithGzip(String data) throws IOException {
        if (isEmpty(data)) {
            return null;
        }
        ByteArrayOutputStream bos = null;
        GZIPOutputStream gos = null;
        try {
            bos = new ByteArrayOutputStream(data.length());
            gos = new GZIPOutputStream(bos);
            gos.write(data.getBytes());
            gos.close();
            byte[] compressed = bos.toByteArray();
            return DigestUtils.toHexString(compressed, 0, compressed.length);
        } catch (Throwable t) {
            // ignore
            Log.d("Thread", t.getMessage());
        } finally {
            try {
                if (gos != null) {
                    gos.close();
                }
                if (bos != null) {
                    bos.close();
                }
            } catch (Throwable t) {
                // ignore
            }
        }
        return null;
    }

    public static String decompressWithGzip(String compressed) throws IOException {
        if (isEmpty(compressed)) {
            return null;
        }
        ByteArrayInputStream bis = null;
        GZIPInputStream gzipInputStream = null;
        BufferedReader br = null;
        try {
            byte[] data = DigestUtils.hexStringToBytes(compressed);
            bis = new ByteArrayInputStream(data);
            gzipInputStream = new GZIPInputStream(bis);
            br = new BufferedReader(new InputStreamReader(gzipInputStream, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (Throwable t) {
            // ignore
            Log.d("Thread", t.getMessage());
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
                if (gzipInputStream != null) {
                    gzipInputStream.close();
                }
                if (bis != null) {
                    bis.close();
                }
            } catch (Throwable t) {
                // ignore
            }
        }
        return null;
    }
}

