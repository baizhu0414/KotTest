package com.example.ktclstest

interface OverrideExample {
    val count: Int
}

// 属性覆盖（可以用var覆盖val）
class Bar1(override var count: Int) : OverrideExample

class Bar2():OverrideExample {
    override var count: Int = 33
        get() = field // 使用幕后字段，否则上面的默认初始化报错
        set(value) {field = value}

    private var tmp:Int ?= null
        get() = field
        // 调用 setter：当执行 tmp = 5 时，会触发 setter 方法
        // 进入 setter：执行 set(value) { tmp = value }
        // 再次赋值：tmp = value 会再次调用 setter 方法
        // 无限循环：重复步骤 2-3 形成无限递归，最终导致 StackOverflowError
//        set(value) {
//            tmp = value
//        }

        // 调用 setter 时：
        // 1. 将 10 赋值给幕后字段 field
        // 2. 不会触发新的 setter 调用
        set(value) {
            field = value
        }
}


class Bar3() : OverrideExample {
    // 幕后属性 (对内可修改)
    private var _count: Int = 33

    // 对外暴露只读属性
    override val count: Int
        get() = _count  // 通过 getter 暴露值

}
