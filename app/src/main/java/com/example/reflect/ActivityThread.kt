package com.example.reflect

import android.content.Context
import android.os.Build
import android.os.Handler
import android.util.Log
import com.example.provider.ProviderInstallProxy
import java.lang.reflect.AccessibleObject.setAccessible

object ActivityThread : Reflect("android.app.ActivityThread") {

    val mH = getField<Handler>("mH")

    val currentActivityThread = getStaticMethod<Any>("currentActivityThread")

    val installContentProvidersMethod = try {
        Log.i(ProviderInstallProxy.TAG, "installContentProviders 反射完成")
        getMethod<Unit>("installContentProviders", Context::class.java, List::class.java)
    } catch (e: Exception) {
        Log.e(ProviderInstallProxy.TAG, "反射 installContentProviders 失败", e)
        null
    }

}