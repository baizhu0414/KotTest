package com.example.utils;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.example.reflect.Reflect;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class ToolUtil {


    public static String mMsgProcessSuffix = ":push";
    public static final String NODEX_PROCESS_SUFFIX = ":nodex";
    private static final int FLAGS_GET_ONLY_FROM_ANDROID = 0x01000000;

    private ToolUtil() {
    }

    public static void setMessageProcessSuffix(String processSuffix) {
        mMsgProcessSuffix = processSuffix;
    }


    public static Intent getLaunchIntentForPackage(Context context, String packageName) {
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(packageName);
        if (intent == null) {
            return intent;
        }
        if (!intent.hasCategory(Intent.CATEGORY_LAUNCHER)) {
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            Log.d("ToolUtils", "add category LAUNCHER in launch intent");
        }
        // set package to null and add flags so this intent has same
        // behavior with app launcher
        intent.setPackage(null);
        intent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    public static boolean isInstalledApp(Context context, String packageName) {
        boolean installed = false;
        if (null != context && !StringUtils.isEmpty(packageName)) {
            PackageManager pm = context.getPackageManager();
            try {
                if (pm.getPackageInfo(packageName, FLAGS_GET_ONLY_FROM_ANDROID) != null) {
                    installed = true;
                }
            } catch (Exception e) {
                // do nothing.
            }
        }
        return installed;
    }

    public static boolean isInstalledApp(Context context, Intent intent) {
        if (intent == null) {
            return false;
        }
        PackageManager m = context.getPackageManager();
        List<ResolveInfo> list = m.queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        if (list != null && list.size() > 0) {
            return true;
        }
        return false;
    }

    public static void runApplication(Context context, String packageName,
                                      String actionUrl) {

        boolean installed = false;
        if (!StringUtils.isEmpty(packageName)) {
            installed = isInstalledApp(context, packageName);
        }

        if (installed) {
            Intent intent = getLaunchIntentForPackage(context, packageName);
            if (intent != null) {
                context.startActivity(intent);
                return;
            }
        }
        if (!StringUtils.isEmpty(actionUrl)) {
            try {
                Uri webUri = Uri.parse(actionUrl);
                Intent webIt = new Intent(Intent.ACTION_VIEW, webUri);
                context.startActivity(webIt);
                return;
            } catch (Exception e) {
                // ignore
            }
        }
        if (!StringUtils.isEmpty(packageName)) {
            Uri marketUri = Uri.parse("market://details?id="
                    + packageName);
            Intent marketIt = new Intent(Intent.ACTION_VIEW, marketUri);
            marketIt.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                context.startActivity(marketIt);
            } catch (Exception e) {
                // ignore
            }
        }
    }

    public static boolean isHuaweiDevice() {
        boolean flag = false;
        try {
            flag = (!StringUtils.isEmpty(Build.BRAND) && Build.BRAND.toLowerCase(Locale.getDefault()).startsWith("huawei"))
                    || (!StringUtils.isEmpty(Build.MANUFACTURER) && Build.MANUFACTURER.toLowerCase(Locale.getDefault()).startsWith("huawei"));
        } catch (Throwable t) {
            // ignore
        }
        return flag;
    }

    public static boolean isEmui(String emuiInfo) {
        if (TextUtils.isEmpty(emuiInfo)) {
            emuiInfo = getEmuiInfo();
        }
        if (!TextUtils.isEmpty(emuiInfo) && emuiInfo.toLowerCase(Locale.getDefault()).startsWith("emotionui")) {
            return true;
        }
        if (isHuaweiDevice()) {
            return true;
        }
        return false;
    }

    public static String getEmuiInfo() {
        return getSystemProperty("ro.build.version.emui");
    }

    private static String systemProperty = null;

    private static String getSystemProperty(String propName) {
        if (!StringUtils.isEmpty(systemProperty)) {
            return systemProperty;
        }
        String line = null;
        BufferedReader input = null;
        try {
            Process p = Runtime.getRuntime().exec("getprop " + propName);
            final int size = 1024;
            input = new BufferedReader(
                    new InputStreamReader(p.getInputStream()), size);
            line = input.readLine();
            input.close();
            systemProperty = line;
        } catch (Throwable ex) {
            Log.e("ToolUtils", "Unable to read sysprop " + propName, ex);
            return line;
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    Log.e("ToolUtils", "Exception while closing InputStream", e);
                }
            }
        }
        return line;
    }

    /**
     * check current ROM is MIUI or not
     */
    public static boolean sIsMiui = false;
    public static boolean sIsInited = false;

    public static boolean isMiui() {
        if (!sIsInited) {
            try {
                Class<?> clz = Class.forName("miui.os.Build");
                if (clz != null) {
                    sIsMiui = true;
                    sIsInited = true;
                    return sIsMiui;
                }
            } catch (Exception e) {
                // ignore
            }
            sIsInited = true;
        }
        return sIsMiui;
    }

    public static boolean isFlyme() {
        if ((!StringUtils.isEmpty(Build.DISPLAY) && Build.DISPLAY.indexOf("Flyme") >= 0)
                || (!StringUtils.isEmpty(Build.USER) && Build.USER.equals("flyme"))) {
            return true;
        }
        return false;
    }



    /**
     * add app shortcut
     * <uses-permission android:name="com.android.launcher.permission.INSTALL_SHORTCUT"/>
     */
    public static void installShortcut(Context context, String pkgName) {
        if (context == null) {
            return;
        }
        try {
            Intent intent = getLaunchIntentForPackage(context, pkgName);
            if (intent == null) {
                return;
            }
            PackageManager pm = context.getPackageManager();
            ApplicationInfo info = pm.getApplicationInfo(pkgName, 0);
            Resources res = pm.getResourcesForApplication(info);
            String iconName = res.getResourceName(info.icon);
            CharSequence label = null;
            try {
                int id = pm.getActivityInfo(intent.getComponent(), 0).labelRes;
                if (id > 0) {
                    label = res.getString(id);
                }
            } catch (Resources.NotFoundException e) {
                label = pm.getApplicationLabel(info);
            }
            if (label == null) {
                return;
            }
            Intent addShortcut = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");
            addShortcut.putExtra(Intent.EXTRA_SHORTCUT_NAME, label);
            addShortcut.putExtra(Intent.EXTRA_SHORTCUT_INTENT, intent);
            addShortcut.putExtra("launch_from", 1);
            Intent.ShortcutIconResource iconRes = new Intent.ShortcutIconResource();
            iconRes.packageName = pkgName;
            iconRes.resourceName = iconName;
            addShortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconRes);
            addShortcut.putExtra("duplicate", false);
            context.sendBroadcast(addShortcut);
        } catch (Exception e) {
            android.util.Log.w("ToolUtils", "addAppShortcut failed: " + e);
        }
    }

    /**
     * remove the specified dir recursively
     *
     * @param dir
     * @throws Exception
     */
    public static void clearDir(String dir) throws Exception {
        File file = new File(dir);
        if (!file.exists()) {
            return;
        }
        File[] subs = file.listFiles();
        int length = subs != null ? subs.length : 0;
        for (int i = 0; i < length; i++) {
            if (subs[i].isDirectory()) {
                removeDir(subs[i].getAbsolutePath());
            } else if (subs[i].isFile()) {
                subs[i].delete();
            }
        }
    }

    /**
     * remove the specified dir recursively
     *
     * @param dir
     * @throws Exception
     */
    public static void removeDir(String dir) throws Exception {
        File file = new File(dir);
        if (file.exists() && file.isDirectory()) {
            File[] subs = file.listFiles();
            int length = subs != null ? subs.length : 0;
            for (int i = 0; i < length; i++) {
                if (subs[i].isDirectory()) {
                    removeDir(subs[i].getAbsolutePath());
                } else {
                    subs[i].delete();
                }
            }
            file.delete();
        }
    }

    /**
     * remove the specified dir recursively
     *
     * @param dir
     * @throws Exception
     */
    public static void clearDir(String dir, Set<String> reserves)
            throws Exception {
        File file = new File(dir);
        if (!file.exists()) {
            return;
        }
        File[] subs = file.listFiles();
        int length = subs != null ? subs.length : 0;
        for (int i = 0; i < length; i++) {
            if (subs[i].isDirectory()) {
                removeDir(subs[i].getAbsolutePath(), reserves);
            } else if (subs[i].isFile()) {
                String name = subs[i].getName();
                if (reserves == null || !reserves.contains(name)) {
                    subs[i].delete();
                }
            }
        }
    }

    /**
     * remove the specified dir recursively
     *
     * @param dir
     * @throws Exception
     */
    public static void removeDir(String dir, Set<String> reserves)
            throws Exception {
        // 定义文件路径
        File file = new File(dir);
        // 判断是文件还是目录
        if (file.exists() && file.isDirectory()) {
            File[] subs = file.listFiles();
            int length = subs != null ? subs.length : 0;
            for (int i = 0; i < length; i++) {
                if (subs[i].isDirectory()) {
                    removeDir(subs[i].getAbsolutePath(), reserves);
                } else {
                    String name = subs[i].getName();
                    if (reserves == null || !reserves.contains(name)) {
                        subs[i].delete();
                    }
                }
            }
            // note, reserve the dir
            // file.delete();
        }
    }

    /**
     * get directory size recursively
     */
    public static long getDirectorySize(File dir, boolean followSymbolic) {
        long total = 0L;
        try {
            if (!dir.exists() && dir.isDirectory()) {
                return total;
            }
            if (!followSymbolic) {
                //todo: check symbolic
            }
            File[] subs = dir.listFiles();
            final int length = subs != null ? subs.length : 0;
            for (int i = 0; i < length; i++) {
                File f = subs[i];
                if (f.isDirectory()) {
                    total += getDirectorySize(f, followSymbolic);
                } else if (f.isFile()) {
                    total += f.length();
                }
            }
        } catch (Throwable t) {
            //
        }
        return total;
    }

    /**
     * request to add image to gallery
     */
    public static void addImageMedia(Context context, String imagePath) {
        try {
            Uri uri = Uri.fromFile(new File(imagePath));
            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri);
            context.sendBroadcast(intent);
        } catch (Exception e) {
            Log.w("ToolUtils", "add image media exception: " + e);
        }
    }

    public static void addImageMedia2(Context context, String imagePath) {
        try {
            MyMediaScannerConnectionClient mc;
            mc = new MyMediaScannerConnectionClient(context, imagePath);
            mc.startScan();
        } catch (Exception e) {
            // ignore
        }
    }

    public static class MyMediaScannerConnectionClient implements MediaScannerConnectionClient {

        private MediaScannerConnection conn;
        private Context mContext;
        private String mImagePath;

        public MyMediaScannerConnectionClient(Context context, String imagePath) {
            mContext = context;
            mImagePath = imagePath;
        }

        public void startScan() {
            if (conn != null && conn.isConnected()) {
                conn.disconnect();
            }
            conn = new MediaScannerConnection(mContext, this);
            conn.connect();
        }

        @Override
        public void onMediaScannerConnected() {
            try {
                conn.scanFile(mImagePath, "image/*");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onScanCompleted(String path, Uri uri) {
            if (conn != null) {
                if (conn.isConnected()) {
                    conn.disconnect();
                }
                conn = null;
            }
        }
    }

    public static boolean isPackageMatchApk(Context context, String apkPath, String packageName) {
        boolean result = false;
        if (context == null || StringUtils.isEmpty(apkPath) || StringUtils.isEmpty(packageName)) {
            return result;
        }
        try {
            File apkFile = new File(apkPath);
            if (apkFile.exists()) {
                PackageManager pm = context.getPackageManager();
                PackageInfo packageInfo = pm.getPackageArchiveInfo(apkFile.getAbsolutePath(), PackageManager.GET_ACTIVITIES);
                if (packageInfo == null) {
                    return result;
                }
                String apkPackageName = packageInfo.packageName;
                if (!apkPackageName.equals(packageName)) {
                    return result;
                }
                int apkVersionCode = packageInfo.versionCode;
                PackageInfo installPackageInfo = null;
                try {
                    installPackageInfo = context.getPackageManager().getPackageInfo(
                            packageName, PackageManager.GET_ACTIVITIES);
                } catch (PackageManager.NameNotFoundException e) {
                    installPackageInfo = null;
                }
                if (installPackageInfo == null) {
                    result = false;
                } else {
                    int installVersionCode = installPackageInfo.versionCode;
                    if (apkVersionCode == installVersionCode) {
                        result = true;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static boolean isApkInstalled(Context context, String apkPath) {
        boolean result = false;
        if (context == null || apkPath == null || StringUtils.isEmpty(apkPath)) {
            return result;
        }
        try {
            File apkFile = new File(apkPath);
            if (apkFile.exists()) {
                PackageManager pm = context.getPackageManager();
                PackageInfo packageInfo = pm.getPackageArchiveInfo(apkFile.getAbsolutePath(), PackageManager.GET_ACTIVITIES);
                if (packageInfo == null) {
                    return result;
                }
                String packageName = packageInfo.packageName;
                int versionCode = packageInfo.versionCode;
                PackageInfo installPackageInfo = null;
                try {
                    installPackageInfo = context.getPackageManager().getPackageInfo(
                            packageName, PackageManager.GET_ACTIVITIES);
                } catch (PackageManager.NameNotFoundException e) {
                    installPackageInfo = null;
                }
                if (installPackageInfo == null) {
                    result = false;
                } else {
                    int installVersionCode = installPackageInfo.versionCode;
                    if (versionCode <= installVersionCode) {
                        result = true;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static boolean isSubProcess(Context context, String subProcessSuffix) {
        String processName = getCurProcessName(context);
        if (processName != null && processName.endsWith(subProcessSuffix)) {
            return true;
        }
        return false;
    }

    public static boolean isMessageProcess(Context context) {
        String processName = getCurProcessName(context);
        if (processName != null && processName.endsWith(mMsgProcessSuffix)) {
            return true;
        }
        return false;
    }

    public static boolean isNoDexProcess(Context context) {
        String processName = getCurProcessName(context);
        if (processName != null && processName.endsWith(NODEX_PROCESS_SUFFIX)) {
            return true;
        }
        return false;
    }

    public static boolean isMainProcess(Context context) {
        String processName = getCurProcessName(context);
        if (processName != null && processName.contains(":")) {
            return false;
        }
        return (processName != null && processName.equals(context.getPackageName()));
    }

    public static long isMainProcessRetId(Context context) {
        boolean main = isMainProcess(context);
        if (main) {
            return Thread.currentThread().getId();
        }
        return 0;
    }

    private static String sCurProcessName = null;

    public static String getCurProcessName(Context context) {
        String procName = sCurProcessName;
        if (!StringUtils.isEmpty(procName)) {
            return procName;
        }
        try {
            int pid = android.os.Process.myPid();
            ActivityManager mActivityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            for (ActivityManager.RunningAppProcessInfo appProcess : mActivityManager.getRunningAppProcesses()) {
                if (appProcess.pid == pid) {
                    Log.d("Process", "processName = " + appProcess.processName);
                    sCurProcessName = appProcess.processName;
                    return sCurProcessName;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        sCurProcessName = getCurProcessNameFromProc();
        return sCurProcessName;
    }

    private static String getCurProcessNameFromProc() {
        BufferedReader cmdlineReader = null;
        try {
            cmdlineReader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(
                            "/proc/" + android.os.Process.myPid() + "/cmdline"),
                    "iso-8859-1"));
            int c;
            StringBuilder processName = new StringBuilder();
            while ((c = cmdlineReader.read()) > 0) {
                processName.append((char) c);
            }
            Log.d("Process", "get processName = " + processName.toString());
            return processName.toString();
        } catch (Throwable e) {
            // ignore
        } finally {
            if (cmdlineReader != null) {
                try {
                    cmdlineReader.close();
                } catch (Exception e) {
                    // ignore
                }
            }
        }
        return null;
    }

    /**
     * 判断当前应用程序处于前台还是后台
     */
    public static boolean isApplicationForeground(Context context, String packageName) {
        return ProcessLifecycleOwner.get().getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED);
    }


    public static void deleteShortCut(Context context, Intent intent, String shortCutName) {
        if (context == null || intent == null
                || StringUtils.isEmpty(shortCutName)) {
            return;
        }
        try {
            Intent target = intent;
            Intent shortCutIntent = new Intent("com.android.launcher.action.UNINSTALL_SHORTCUT");
            //快捷方式的名称
            shortCutIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, target);
            shortCutIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, shortCutName);
            shortCutIntent.putExtra("duplicate", false);
            context.sendBroadcast(shortCutIntent);
            Log.d("launcher_ad", "deleteShortCut intent " + intent.toUri(0));
        } catch (Exception e) {
            //ignore
        }
    }

    /**
     * 调起打电话界面
     *
     * @param context
     * @param num
     */
    public static void startPhoneScreen(Context context, String num) {
        if (context == null || StringUtils.isEmpty(num)) {
            return;
        }
        try {
            Uri uri = Uri.parse("tel:" + Uri.encode(num));
            Intent intent = new Intent(Intent.ACTION_DIAL, uri);
            if (!(context instanceof Activity)) {
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            context.startActivity(intent);
        } catch (Exception e) {
            // ignore
        }
    }

    /**
     * 打开已安装应用
     *
     * @param context
     * @param packageName
     */
    public static void openInstalledApp(Context context, String packageName) {
        if (context == null || StringUtils.isEmpty(packageName)) {
            return;
        }
        try {
            PackageManager pm = context.getPackageManager();
            Intent intent = pm.getLaunchIntentForPackage(packageName);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            // ignore
        }
    }

    /**
     * 获取provider的authority
     *
     * @param context
     * @param providerName
     * @return
     */
    public static String getProviderAuthority(Context context, String providerName) {
        if (context == null || StringUtils.isEmpty(providerName)) {
            return null;
        }
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_PROVIDERS);
            ProviderInfo[] providerInfos = packageInfo.providers;
            for (ProviderInfo info : providerInfos) {
                if (providerName.equals(info.name)) {
                    return info.authority;
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }


    public static String handleZipEntryGetNameLeak(String name) {
        if (StringUtils.isEmpty(name)) {
            return name;
        }
        try {
            Log.d("Process", "before handle = " + name);
            name = name.replaceAll(".." + File.separator, "");
            Log.d("Process", "after handle = " + name);
        } catch (Exception e) {
            // ignore
        }
        return name;
    }


    public static String getCacheDirPath(Context context) throws NullPointerException {
        if (context == null) {
            throw new NullPointerException("Context is NUll");
        }
        String cacheDir = null;
        try {
            if (context.getCacheDir() != null) {
                cacheDir = context.getCacheDir().getPath();
            } else {
                File cacheDirFile = context.getDir("/data/data/" + context.getPackageName() + "/cache/", Context.MODE_PRIVATE);
                if (cacheDirFile != null) {
                    cacheDir = cacheDirFile.getPath();
                } else {
                    cacheDir = null;
                }
            }
        } catch (Throwable t) {
            // ignore
        }
        if (StringUtils.isEmpty(cacheDir)) {
            throw new NullPointerException("Cannot Create Cache Dir");
        }
        return cacheDir;
    }

    public static String getFilesDirPath(Context context) throws NullPointerException {
        if (context == null) {
            throw new NullPointerException("Context is NUll");
        }
        String filesDir = null;
        try {
            if (context.getFilesDir() != null) {
                filesDir = context.getFilesDir().getPath();
            } else {
                File filesDirFile = context.getDir("/data/data/" + context.getPackageName() + "/files/", Context.MODE_PRIVATE);
                if (filesDirFile != null) {
                    filesDir = filesDirFile.getPath();
                } else {
                    filesDir = null;
                }
            }
        } catch (Throwable t) {
            // ignore
        }
        if (StringUtils.isEmpty(filesDir)) {
            throw new NullPointerException("Cannot Create Files Dir");
        }
        return filesDir;
    }

    /**
     * 判断服务是否在运行
     *
     * @param context
     * @param packageName
     * @param serviceName
     * @return
     */
    public static boolean isServiceRunning(Context context, String packageName, String serviceName) {
        if (context == null || StringUtils.isEmpty(packageName)
                || StringUtils.isEmpty(serviceName)) {
            return false;
        }
        boolean isRunning = false;
        try {
            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningServiceInfo> services = activityManager.getRunningServices(Integer.MAX_VALUE);
            if (services != null && services.size() > 0) {
                for (ActivityManager.RunningServiceInfo service : services) {
                    if (packageName.equals(service.service.getPackageName()) && serviceName.equals(service.service.getClassName())) {
                        isRunning = true;
                        break;
                    }
                }
            }
        } catch (Throwable t) {
            // ignore
        }

        return isRunning;
    }

    public static void trySetStatusBarWithFullScreen(Window window) {
        if (window == null) {
            return;
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                        | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
                window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.setStatusBarColor(Color.TRANSPARENT);
            }
        } catch (Throwable t) {
            // ignore
            t.printStackTrace();
        }
    }

    /**
     * @param context 当前Context
     * @param window 当前window(Activity or Dialog window)
     * @param colorRes status bar bg color
     * @param isLightStatusBarOn 是:开启 status bar 灰色模式; 否:开启 status bar 白色模式
     */
    private static boolean sCanSetStatusBar = true;

    public static void setCanSetStatusBar(boolean val) {
        sCanSetStatusBar = val;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static void trySetStatusBar(Context context, Window window,
                                       int colorRes, boolean isLightStatusBarOn) {
        try {
            if (context == null || window == null) {
                return;
            }
            if (!sCanSetStatusBar) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                return;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (isMiui()) {
                    if (isLightStatusBarOn) {
                        setMiuiStatusBarDarkMode(false, window);
                    } else {
                        setMiuiStatusBarDarkMode(true, window);
                    }
                } else if (isFlyme()) {
                    if (isLightStatusBarOn) {
                        setFlymeStatusBarDarkMode(false, window);
                    } else {
                        setFlymeStatusBarDarkMode(true, window);
                    }
                } else {
                    final View decorView = window.getDecorView();
                    final int systemUiVisibility = decorView.getSystemUiVisibility();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (isLightStatusBarOn) {
                            decorView.setSystemUiVisibility(
                                    systemUiVisibility & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                        } else {
                            decorView.setSystemUiVisibility(
                                    systemUiVisibility | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                        }
                    }
                }
                // clear FLAG_TRANSLUCENT_STATUS flag:
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                // add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                // finally change the color
                window.setStatusBarColor(context.getResources().getColor(colorRes));
            }
        } catch (Throwable t) {
            // ignore
            t.printStackTrace();
        }
    }

    private static void setMiuiStatusBarDarkMode(boolean darkmode, Window window) {
        try {
            Class<? extends Window> clazz = window.getClass();
            Class<?> layoutParams = Class.forName("android.view.MiuiWindowManager$LayoutParams");
            Field field = layoutParams.getField("EXTRA_FLAG_STATUS_BAR_DARK_MODE");
            int darkModeFlag = field.getInt(layoutParams);
            Method extraFlagField = clazz.getMethod("setExtraFlags", int.class, int.class);
            extraFlagField.invoke(window, darkmode ? darkModeFlag : 0, darkModeFlag);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    @SuppressWarnings("checkstyle:MagicNumber")
    private static void setFlymeStatusBarDarkMode(boolean darkMode, Window window) {
        try {
            WindowManager.LayoutParams lp = window.getAttributes();
            Field meizuFlags = WindowManager.LayoutParams.class.getDeclaredField("meizuFlags");
            if (darkMode) {
                int newFlag = meizuFlags.getInt(lp) | 0x200;
                meizuFlags.set(lp, newFlag);
            } else {
                int newFlag = meizuFlags.getInt(lp) & ~0x200;
                meizuFlags.set(lp, newFlag);
            }
            window.setAttributes(lp);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

}
