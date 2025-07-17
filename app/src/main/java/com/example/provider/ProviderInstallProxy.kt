package com.example.provider

import android.content.Context
import android.content.pm.ProviderInfo
import android.os.Build
import android.util.Log
import com.example.reflect.ActivityThread
import com.example.reflect.AppBindData

object ProviderInstallProxy {
    const val TAG = "ProviderInstallProxy"

    // 白名单：需要正常初始化的 Provider在此处添加即可（通过 authority 标识，可根据实际需求修改）
    val PROVIDER_WHITELIST: Set<String> = setOf()

    // 延迟安装的 Provider（通过 authority 标识，此处仅作为标记，会自动添加和删除）
    var PROVIDER_DELAY_LIST = mutableSetOf(
        "com.lemon.lvoverseas.bdinstall.provider",
        "com.lemon.lvoverseas.firebaseinitprovider",
        "com.lemon.lvoverseas.mobileadsinitprovider",
        "com.lemon.lvoverseas.plumber-installer",
        "com.lemon.lvoverseas.depths.i18n.account.provider.main",
        "com.lemon.lvoverseas.com.squareup.picasso",
        "com.lemon.lvoverseas.app2app.TestContentProvider",
        "com.lemon.lvoverseas.AudienceNetworkContentProvider",
        "com.lemon.lvoverseas.push.SHARE_PROVIDER_AUTHORITY",
        "com.lemon.lvoverseas.auth_token.SHARE_PROVIDER_AUTHORITY",
        "com.lemon.lvoverseas.TicketGuardProvider",
        "com.lemon.lvoverseas.saitama.provider",
        "com.lemon.lvoverseas.FacebookInitProvider",
        "com.facebook.app.FacebookContentProvider163543514909045",
        "com.lemon.lvoverseas.doctor",
        "com.lemon.lvoverseas.WsChannelMultiProcessSharedProvider",
        "com.lemon.lvoverseas.CodeLocatorProvider",
        "com.lemon.lvoverseas.provider",
        "com.lemon.lvoverseas.DebugDBInitProvider",
        "com.lemon.lvoverseas.push.settings",
        "com.lemon.lvoverseas.apm",
        "com.lemon.lvoverseas.StarkInitProvider",

        // AndroidX Startup 是 AndroidX 库的基础组件（默认版本 1.0.0+），许多常用库（如 Room、WorkManager、Firebase 等）都依赖它，因此我们不能将其在子线程初始化
        // 如果放入白名单，将会触发IPC调用，因此建议此处初始化在主线程调用而不是采用白名单放行的方式。
        // 此处初始化的时候涉及到InitializationProvider- ProfileInstallerInitializer为了优化性能，需要与 UI 渲染同步，因此它尝试获取 Choreographer 的实例- 强依赖于主线程的 Looper
        "com.lemon.lvoverseas.androidx-startup",
        "com.lemon.lvoverseas.applovininitprovider",
        "com.lemon.lvoverseas.fileprovider",
        "com.lemon.lvoverseas.calidge.fileprovider",
        "com.lemon.lvoverseas.push.file_provider",
    )

    // 暂存非白名单的 Provider（待延迟安装）
    var providersToDelay: MutableList<ProviderInfo> = mutableListOf()

    @JvmStatic
    fun clearActivityThreadProviders() {
        try {
            // 获取 ActivityThread 实例
            val activityThread = ActivityThread.currentActivityThread?.call() ?: return

            // 获取 AppBindData 对象（mBoundApplication 字段）
            val mBoundApplicationField = activityThread.javaClass.getDeclaredField("mBoundApplication")
            mBoundApplicationField.isAccessible = true
            val appBindData = mBoundApplicationField.get(activityThread) ?: return

            // 获取并复制 providers 列表
            val originalProviders = AppBindData.providers?.getValue(appBindData) as? List<ProviderInfo>

            if (originalProviders.isNullOrEmpty()) {
                Log.i(TAG, "没有需要处理的 Provider")
                return
            }
            // 过滤 providers 列表，保留白名单
            val whiteListProviders = mutableListOf<ProviderInfo>()
            val delayedProviders = mutableListOf<ProviderInfo>()
            originalProviders.forEach { providerInfo ->
                val authority = providerInfo.authority

                if (authority != null && PROVIDER_WHITELIST.contains(authority)) {
                    // 白名单Provider
                    whiteListProviders.add(providerInfo)
                    Log.i(TAG, "白名单正常初始化：$authority")
                } else {
                    // 非白名单Provider，加入延迟列表并记录
                    delayedProviders.add(providerInfo)

                    if (authority != null) {
                        PROVIDER_DELAY_LIST.add(authority)
                        Log.i(TAG, "加入延迟列表：$authority")
                    } else {
                        Log.w(TAG, "发现authority为null的Provider: ${providerInfo.name}")
                    }
                }
            }
            providersToDelay = delayedProviders.toMutableList()
            AppBindData.providers.setValue(appBindData, whiteListProviders)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "处理 Provider 失败", e)
        }
    }

    @JvmStatic
    fun installDelayedProviders(app: Context?) {
        if (app == null) {
            Log.w(TAG, "安装provider失败: app is null.")
            return
        }
        if (providersToDelay.isEmpty()) {
            Log.i(TAG, "没有需要延迟安装的providers.")
            return
        }

        // 再次判断防止com.lemon.lvoverseas.androidx-startup拉起其他delayedProvider的安装，导致循环安装
        val iterator = providersToDelay.iterator()
        while (iterator.hasNext()) {
            val providerInfo = iterator.next()
            if (isProviderInstalled(providerInfo.authority)) {
                iterator.remove()
            }
        }

        Log.i(TAG, "开始安装 ${providersToDelay.size} 个延迟 Provider.")
        try {
            val activityThreadInstance = try {
                ActivityThread.currentActivityThread?.call()
            } catch (e: Exception) {
                Log.e(TAG, "获取 ActivityThread 实例失败", e)
                return
            }

            // 根据版本选择正确的方法签名
            Log.i(TAG, "开始安装provider.调用方法：installContentProviders")
            ActivityThread.installContentProvidersMethod?.call(
                activityThreadInstance,
                app,
                providersToDelay
            )

            Log.i(TAG, "延迟安装provider调用完成.")
        } catch (e: Throwable) {
            Log.e(TAG, "延迟安装provider失败.", e)
        } finally {
            // do nothing
        }
    }

    private fun isProviderInstalled(authority: String): Boolean {
        /**
         * 反射检查 Provider 是否已安装，这是为了防止重复创建导致的内存泄露。
         */
        return try {
            // 1. 获取 ActivityThread 实例
            val activityThreadClass = Class.forName("android.app.ActivityThread")
            val currentActivityThreadMethod = activityThreadClass.getDeclaredMethod("currentActivityThread")
            currentActivityThreadMethod.isAccessible = true
            val activityThread = currentActivityThreadMethod.invoke(null)

            // 2. 获取 mProviderMap 字段，它是已安装Provider的映射
            val mProviderMapField = activityThreadClass.getDeclaredField("mProviderMap")
            mProviderMapField.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val mProviderMap = mProviderMapField.get(activityThread) as Map<*, *>

            // 3. 遍历 mProviderMap 的键 (ProviderKey)，检查 authority 是否匹配
            for (key in mProviderMap.keys) {
                if (key != null) {
                    // 通过反射获取 ProviderKey 的 authority 字段
                    val authorityField = key.javaClass.getDeclaredField("authority")
                    authorityField.isAccessible = true
                    val providerAuthority = authorityField.get(key) as String
                    if (providerAuthority == authority) {
                        Log.i(TAG, "找到匹配的 Provider: $authority")
                        return true
                    }
                }
            }

            Log.i(TAG, "Provider: $authority 未安装")
            false
        } catch (e: Exception) {
            Log.e(TAG, "反射检查 Provider 失败: $authority", e)
            false
        }
    }

}