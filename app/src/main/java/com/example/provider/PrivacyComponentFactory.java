package com.example.provider;

import android.app.AppComponentFactory;
import android.app.Application;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * 防止API 28+ 反射获取provider信息失败的问题
 */
@RequiresApi(api = Build.VERSION_CODES.P)
public class PrivacyComponentFactory extends AppComponentFactory { // <-- 正确的父类

    @NonNull
    @Override
    public ContentProvider instantiateProvider(
            @NonNull ClassLoader cl,
            @NonNull String className
    ) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        ProviderInfo providerInfo = getProviderInfoByClassName(className);
        if (providerInfo == null || providerInfo.authority == null) {
            Log.i(ProviderInstallProxy.TAG, "无法获取provider信息，走原路径" + className);
            return super.instantiateProvider(cl, className);
        }

        String authority = providerInfo.authority;
        if (!ProviderInstallProxy.INSTANCE.getPROVIDER_WHITELIST().contains(authority)) {
            ProviderInstallProxy.INSTANCE.getProvidersToDelay().add(providerInfo);
            Log.i(ProviderInstallProxy.TAG, "准备延迟安装provider: " + authority);
            return new DummyContentProvider();
        }
        Log.i(ProviderInstallProxy.TAG, "白名单走原路径" + className);
        return super.instantiateProvider(cl, className);
    }

    private ProviderInfo getProviderInfoByClassName(String className) {
        // 你的反射逻辑是正确的
        try {
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Field currentActivityThreadField = activityThreadClass.getDeclaredField("sCurrentActivityThread");
            currentActivityThreadField.setAccessible(true);
            Object activityThread = currentActivityThreadField.get(null);
            Field boundApplicationField = activityThreadClass.getDeclaredField("mBoundApplication");
            boundApplicationField.setAccessible(true);
            Object boundApplication = boundApplicationField.get(activityThread);
            Field providersField = boundApplication.getClass().getDeclaredField("providers");
            providersField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<ProviderInfo> allProviders = (List<ProviderInfo>) providersField.get(boundApplication);
            for (ProviderInfo info : allProviders) {
                if (info.name.equals(className)) {
                    Log.i(ProviderInstallProxy.TAG, "获取ProviderInfo: " + info.authority + " ?= className:" + className);
                    return info;
                }
            }
        } catch (Exception e) {
            // 在生产环境中建议记录此异常用于调试
            Log.e(ProviderInstallProxy.TAG, "获取 ProviderInfo 失败", e);
        }
        return null;
    }

    /**
     * 一个健壮的、用于占位的 Provider.
     */
    public static class DummyContentProvider extends ContentProvider {
        @Override
        public boolean onCreate() {
            return true;
        }

        @Override
        public String getType(@NonNull Uri uri) {
            // 好的实践：返回一个合法的 MIME 类型
            return "vnd.android.cursor.dir/null";
        }

        @Override
        public Cursor query(@NonNull Uri uri, String[] p, String s, String[] sa, String so) {
            // 好的实践：返回一个空游标，而不是 null。此时checkContentProviderPermissionLocked会直接返回false
            // 当外部应用尝试访问 Provider 时，AMS 会执行上述检查。若 exported 为 false 或调用者权限不足，IPC 请求会被直接拒绝。
            return new MatrixCursor(new String[0]);
        }

        // 其他方法可以保持空实现
        @Override public Uri insert(@NonNull Uri uri, ContentValues v) { return null; }
        @Override public int delete(@NonNull Uri u, String s, String[] sa) { return 0; }
        @Override public int update(@NonNull Uri u, ContentValues v, String s, String[] sa) { return 0; }
    }

    public static void setAppComponentFactory() {
        // 高版本通过PrivacyComponentFactory!!!并且不能再attachBaseContext中放置application未初始化导致反射出问题。
        try {
            // 获取 ActivityThread 和 Application 实例
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Method currentActivityThreadMethod = activityThreadClass.getDeclaredMethod("currentActivityThread");
            currentActivityThreadMethod.setAccessible(true);
            Object activityThread = currentActivityThreadMethod.invoke(null);
            Log.i(ProviderInstallProxy.TAG, "ActivityThread: " + activityThread);

            Method getApplicationMethod = activityThreadClass.getDeclaredMethod("getApplication");
            getApplicationMethod.setAccessible(true);
            Application application = (Application) getApplicationMethod.invoke(activityThread);

            Field loadedApkField = Application.class.getDeclaredField("mLoadedApk");
            loadedApkField.setAccessible(true);
            Object loadedApk = loadedApkField.get(application);
            Log.i(ProviderInstallProxy.TAG, "LoadedApk: " + loadedApk);

            // 根据 Android 版本选择不同的字段名
            Field factoryField;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // Android 9+ 使用 mAppComponentFactory（1+手机实效，似乎是frame层修改了，无法反射成功）
                factoryField = loadedApk.getClass().getDeclaredField("mAppComponentFactory");
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Android 8 使用 mFactory
                factoryField = loadedApk.getClass().getDeclaredField("mFactory");
            } else {
                Log.e(ProviderInstallProxy.TAG, "不支持的 Android 版本");
                return;
            }

            factoryField.setAccessible(true);
            factoryField.set(loadedApk, new PrivacyComponentFactory());
            Log.i(ProviderInstallProxy.TAG, "ComponentFactory: " + factoryField);

            Log.i(ProviderInstallProxy.TAG, "手动设置ComponentFactory成功");
        } catch (Exception e) {
            Log.e(ProviderInstallProxy.TAG, "手动设置ComponentFactory失败", e);
            // 使用默认实现
        }
    }
}
