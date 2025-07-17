package com.example.reflect

import com.vega.hook.reflect.EasyReflect

/**
 * desc : 反射基类，用于集中管理同一个类的反射字段or方法
 *
 * @param clsName 反射的类名
 * @param lateInit 是否懒加载
 */
open class Reflect(private val clsName: String, lateInit: Boolean = false) {

    private lateinit var clsReflect: EasyReflect
    lateinit var type: Class<*>

    init {
        /**
         * 懒加载的情况下类初始化不进行反射
         */
        if (!lateInit) {
            clsReflect = EasyReflect.create(clsName)
            type = clsReflect.clazz
        }
    }

    /**
     * 懒加载需要传入classLoader后才能进行实际字段的反射
     */
    open fun init(classLoader: ClassLoader) {
        clsReflect = EasyReflect.create(clsName, classLoader)
        type = clsReflect.clazz
    }

    protected fun <T> getConstructor(vararg types: Class<*>): EasyReflect.ConstructorReflect<T>? {
        return clsReflect.getConstructor(*types)
    }

    protected fun <T> getField(name: String): EasyReflect.FieldReflect<T>? {
        return clsReflect.getField(name)
    }

    protected fun <T> getStaticField(name: String): EasyReflect.StaticFieldReflect<T>? {
        return clsReflect.getStaticField(name)
    }

    protected fun <T> getMethod(name: String, vararg types: Class<*>): EasyReflect.MethodReflect<T>? {
        return clsReflect.getMethod(name, *types)
    }

    protected fun <T> getStaticMethod(name: String, vararg types: Class<*>): EasyReflect.StaticMethodReflect<T>? {
        return clsReflect.getStaticMethod(name, *types)
    }

    protected fun <T> getStaticMethod(clazz: Class<*>, methodName: String, vararg args: Any?): EasyReflect.StaticMethodReflect<T>? {
        return clsReflect.getStaticMethod(clazz, methodName, *args)
    }

    fun arrayClass(): Class<*> {
        return clsReflect.arrayClass()
    }
}
