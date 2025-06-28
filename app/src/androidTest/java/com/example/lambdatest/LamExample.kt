package com.example.lambdatest

import android.content.Intent
import com.example.kottest.StockResponse

class LamExample {
    // JvmOverloads 注解可以让编译器默认参数函数供Java调用，kotlin中不需要也可以。
    @JvmOverloads fun read(b: Array<Int>, off:Int=0, len:Int=b.size) {

    }


    fun useRead() {
        read(arrayOf(1,2,3))
        read(arrayOf(1,2,3), 2)
        read(arrayOf(1,2,3), 2, 5)
    }


    /*Lambda：简化匿名内部类*/
//    new 父类/接口() {
//        // 重写方法或添加新方法
//    };
    // Lambda表达式含义：变量能做什么，函数就能做什么。
    val addFun = { a:Int, b:Int -> a + b }
    // compare:(Int, Int)->Int 表示一个函数类型，参数为两个Int，返回值为Int。
    val compareFun = {a:Int, b:Int, compare:(Int, Int)->Int -> {
        if(compare(a,b)>0) a else b
    }}

    fun useFun() {
        val max = compareFun(1,2, addFun)
        println(max)
    }

    // lambda链式调用
    fun useChain() {
        val stockResponse = StockResponse()
        stockResponse.stock?.prices
            ?.filter { it?.open?.let { open->open>100 } == true }
            // map 函数会对过滤后集合中的每个元素应用上述转换函数，把转换后的结果收集到一个新的集合中，这个新集合的元素类型是 Double? ｜。
            ?.map { it?.open }?.forEach { println(it) }
    }

    // apply,also,run,let  使用方法
    fun getNewIntent() = Intent().apply {
        putExtra("key1", 1)
        putExtra("key2", true)
        putExtra("key3", "value")
    }
}