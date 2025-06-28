package com.example.ktclstest

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

// 父类定义
open class KtClsExtend(str: String) {
    open fun v() {
        println("父类可重写方法")
    }
}

// 子类定义
class Student(str: String):KtClsExtend(str) {
    fun process() {
        val student = Student("test")
        println(student is KtClsExtend) // 输出 true，说明 Student 是 KtClsExtend 的子类
    }
}

// 子类定义，不使用主构造函数
@RunWith(JUnit4::class)
class Student2:KtClsExtend {
    constructor():super("") {
        println("子类构造函数")
    }

    constructor(str: String):super(str) {
        println("子类构造函数")
    }

    @Test
    override fun v() {
        println("子类重写方法")
    }
}
