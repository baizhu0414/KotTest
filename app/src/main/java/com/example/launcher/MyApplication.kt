package com.example.launcher

import android.app.Application
import android.content.Context
import android.os.Build
import com.example.component.StartExecutorService
import com.example.daggeruse.network.DaggerNetworkDatabaseComponent
import com.example.provider.ProviderInstallProxy
import com.example.utils.ToolUtil


class MyApplication : Application() {

    private val isMainProcess by lazy {
        ToolUtil.isMainProcess(this@MyApplication)
    }

    override fun attachBaseContext(base: Context?) {
        ProviderInstallProxy.clearActivityThreadProviders()
        super.attachBaseContext(base)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this;

        StartExecutorService.execute {
            if (isMainProcess && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
//                PrivacyComponentFactory.setAppComponentFactory() // 部分手机厂商不支持
            }

        }
        if (isMainProcess) {
            ProviderInstallProxy.installDelayedProviders(this)
        }
    }

    val component by lazy { //自动生成getComponent方法，懒加载，第一次调用时才会执行
        DaggerNetworkDatabaseComponent.builder()
            .context(this)
            .dbName("user_db.db")
//            .databaseModule(DatabaseModule(this, "user_db.db"))
            .build()
    }

    companion object { // 伴生对象，用于‘在类内部’(关键)定义一个静态成员集合，类的所有实例共享这些成员。
        // 模拟static变量
        lateinit var instance: MyApplication
    }
}