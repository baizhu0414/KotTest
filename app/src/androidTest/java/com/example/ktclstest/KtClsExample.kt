package com.example.ktclstest

// 主构造函数
public class KtClsExample(name: String="XX", val age:Int=18) {

    // 次构造函数
    constructor(name: String, parent:String="Parent") : this(name, 0){
        println("次构造函数")
    }

    fun useConstructorParameter() {
        println("使用默认参数val可行:$age")
//        println("使用默认参数var不可行:$name")
    }
}

