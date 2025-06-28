package com.example.candytest

import android.widget.TextView
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader

/**
 * 语法糖
 */
class CandyExample {

    // 延迟初始化(lateinit 适合View，资源适合lazy。因为lazy 可能引起Fragment重建后的内存泄露！)
    // 此外也可使用Optional，但是需要判空。 textView:TextView? = null
    private lateinit var textView: TextView // 难点

    private val inputStream by lazy {
        // 初始化逻辑
        try {
            BufferedReader(InputStreamReader(FileInputStream("input.txt")))
        } catch (e: Exception) {
            // 处理异常
            null
        }
    }

    fun test() {
        // 如 lateinit 延迟初始化变量是public，那么使用前应该检查是否已经初始化
        if(::textView.isInitialized) {
            // 使用inputStream
        }
    }

    // 补充：
    // Pair简洁写法："key" to value
    // 字符串模板："Hello, ${name}!"
    // 强转类型：is as

}

// 单例模式：
//// 标准写法（线程安全，按需初始化）
object Singleton {
    fun doWork() = println("Working...")
}

//// 懒汉式（使用 lazy 延迟初始化）
class LazySingleton private constructor() {
    companion object {
        val instance: LazySingleton by lazy { LazySingleton() }
    }
}
//// 标准单例调用 （直接通过类名访问）
//Singleton.doWork()
//
//// 懒汉式调用
//LazySingleton.instance.doSomething()