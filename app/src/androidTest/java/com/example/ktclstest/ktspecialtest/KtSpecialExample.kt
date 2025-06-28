package com.example.ktclstest.ktspecialtest

import com.example.kottest.Stock

/**
 * 属性定义原则：
 *  1. 公共字段添加const、@JvmField注解，减少get、set方法
 *  2. 不变量使用val，减少set方法。
 *  3. 不需要外部访问一定要加private，否则会生成get、set方法
 */
class KtSpecialExample {
    // Companion Object-》public static final class
    // Enum Class
}

// 1. Data Class
data class DataClassExample(val name: String, val age: Int)

fun main() {
    var user = DataClassExample("John", 25)

    // 解构
    val (name, age) = user
    println("Name: $name, Age: $age")

    // 1. copy:所有基本类型属性都会被复制，如果数据类包含引用类型属性（如 List），copy 后新旧对象会共享该引用。
    // 2. copy不要求var类型，直接创建新age对象
    user = user.copy(age = 30)
//    user.age = 31 // 必须设置user中的属性为var，否则报错

}


// 2. Sealed Class
sealed class StockState {
    // 子类必须在同一个文件中定义，携带Stock信息
    data class Success(val stock: Stock) : StockState()
    // 失败子类：携带异常信息。
    data class Error(val exception: Throwable) : StockState()
}
/*
// 传统写法（代码块体）
fun handleStockState(state: StockState) {
    return when (state) {
        is StockState.Success -> { ... }
        is StockState.Error -> { ... }
    }
}

// 单表达式写法（更简洁）
fun handleStockState(state: StockState) = when (state) {
    is StockState.Success -> { ... }
    is StockState.Error -> { ... }
}

fun handleStockState(state: StockState):Unit = when (state) {
    // 类型检查 + 自动类型转换
    is StockState.Success -> {
        // ✅ 这里 state 自动转换为 Success 类型
        state.stock.prices?.filterNotNull()?.forEach { price ->
            println("开盘价：${price.open ?: "无数据"}")
        }
    }
    is StockState.Error -> {
        // ✅ 自动转换为 Error 类型
        println("错误：${state.exception.message}")
    }
    // 不需要 else，因为密封类已覆盖所有情况
}
* */
//    限制类的继承范围，只能在同一个文件中使用，不能被其他文件继承
//    所有构造函数必须是private。与when结合遍历所有情形。