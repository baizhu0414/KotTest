package com.example.provider

import android.content.Context
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File

// MyFileProvider.kt
class MyFileProvider : FileProvider() {

    companion object {
        // 用于存储自定义配置或状态
        private const val TAG = ProviderInstallProxy.TAG

        fun getAuthority() = "com.example.kottest.myfileprovider"

        fun getUriForFile(context: Context, file: File): Uri {
            return getUriForFile(context, getAuthority(), file) // 调用父类的3参数方法
        }
    }

    override fun onCreate(): Boolean {
        super.onCreate()
        Log.i(TAG, "Provider创建成功")
        return true
    }

}