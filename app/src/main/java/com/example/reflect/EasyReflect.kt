package com.vega.hook.reflect

import android.util.Log
import java.lang.ClassCastException
import java.lang.reflect.AccessibleObject
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.Proxy

/**
 * @author yuankejie
 * date : 2021/1/10
 * desc : 反射封装类
 */
class EasyReflect private constructor(val clazz: Class<*>, private var instance: Any?) {

    // 构造方法操作区
    /**
     * 使用匹配参数的构造函数创建一个对象实例，并生成新的EasyReflect实例返回
     */
    fun instance(vararg args: Any?): EasyReflect? {
        return getConstructor<Any>(*types(*args))?.createReflect(*args)
    }

    /**
     * 根据传入的参数类型匹配对应的构造器
     */
    fun <T> getConstructor(vararg types: Class<*>): ConstructorReflect<T>? {
        val constructor: Constructor<*>? = try {
            clazz.getDeclaredConstructor(*types)
        } catch (e: NoSuchMethodException) {
            var matched: Constructor<*>? = null
            for (constructor in clazz.declaredConstructors) {
                if (match(constructor.parameterTypes, types)) {
                    matched = constructor
                    break
                }
            }
            matched
        }
        return if (constructor != null)
            ConstructorReflect(accessible(constructor), this)
        else null
    }

    /**
     * 获取此类的所有构造函数
     */
    fun getConstructors(): List<ConstructorReflect<*>> {
        val list = mutableListOf<ConstructorReflect<*>>()
        for (constructor in clazz.declaredConstructors) {
            list.add(ConstructorReflect<Any>(constructor, this))
        }
        return list
    }

    // 成员变量操作区
    /**
     * 为指定name的成员变量赋值为value
     */
    fun setField(name: String, value: Any?): EasyReflect {
        getField<Any>(name)?.setValue(value)
        return this
    }

    /**
     * 获取指定字段name的具体值(包括父类中的字段)
     */
    fun <T> getFieldValue(name: String): T? {
        return getField<T>(name)?.getValue()
    }

    /**
     * 为了方便区分,增加getStaticField方法,实际跟直接调用getField一致,只是返回类型不一样
     */
    fun <T> getStaticField(name: String): StaticFieldReflect<T>? {
        var type: Class<*>? = clazz

        val field = try {
            accessible(type!!.getField(name))
        } catch (e: NoSuchFieldException) {
            var find: Field? = null
            do {
                try {
                    find = accessible(type!!.getDeclaredField(name))
                    if (find != null) {
                        break
                    }
                } catch (ignore: NoSuchFieldException) {
                }
                type = type?.superclass
            } while (type != null)

            find
        } ?: return null
        return StaticFieldReflect(field, this)
    }

    /**
     * 根据指定name获取对应的FieldReflect
     */
    fun <T> getField(name: String): FieldReflect<T>? {
        var type: Class<*>? = clazz

        val field = try {
            accessible(type!!.getField(name))
        } catch (e: NoSuchFieldException) {
            var find: Field? = null
            do {
                try {
                    find = accessible(type!!.getDeclaredField(name))
                    if (find != null) {
                        break
                    }
                } catch (ignore: NoSuchFieldException) {
                }
                type = type?.superclass
            } while (type != null)

            find
        } ?: return null
        return FieldReflect(field, this)
    }

    fun <T> transform(name: String): EasyReflect? {
        return getField<T>(name)?.transform()
    }

    /**
     * 获取所有的字段。包括父类的
     */
    fun getFields(): List<FieldReflect<Any>> {
        val list = mutableListOf<FieldReflect<Any>>()
        var type: Class<*>? = clazz
        do {
            for (field in type!!.declaredFields) {
                list.add(FieldReflect(accessible(field), this))
            }
            type = type.superclass
        } while (type != null)
        return list
    }

    // 普通方法操作区
    /**
     * 执行指定name的方法。并返回自身的EasyReflect实例
     */
    fun call(name: String, vararg args: Any?): EasyReflect {
        getMethod<Any>(name, *types(*args))?.callReflect(*args)
        return this
    }

    /**
     * 执行指定name的方法, 并将方法的返回值作为新数据。创建出对应的EasyReflect实例并返回
     *
     * **请注意：指定name的方法必须含有有效的返回值。**
     */
    fun callWithReturn(name: String, vararg args: Any?): EasyReflect? {
        return getMethod<Any>(name, *types(*args))?.callWithReturn(*args)
    }

    /**
     * 为了方便区分,增加getStaticMethod方法,实际跟直接调用getMethod一致,只是返回类型不一样
     */
    fun <T> getStaticMethod(name: String, vararg types: Class<*>): StaticMethodReflect<T>? {
        var find: Method? = null
        var type: Class<*>? = clazz
        while (type != null) {
            try {
                find = type.getDeclaredMethod(name, *types)
                break
            } catch (e: NoSuchMethodException) {
                for (method in type.declaredMethods) {
                    if (method.name == name && match(method.parameterTypes, types)) {
                        find = method
                        break
                    }
                }
                // 如果找不到尝试纯粹匹配方法名找到第一个
                if (find == null) {
                    for (method in type.declaredMethods) {
                        if (method.name == name) {
                            find = method
                            break
                        }
                    }
                }
                type = type.superclass
            }
        }
        return if (find == null) null else StaticMethodReflect(accessible(find), this)
    }

    // 修复版本差异和类型推断问题，Kotlin 1.6+ 中，KClass.javaClass 已被弃用，推荐使用 KClass.java。
    fun <T> getStaticMethod(clazz: Class<*>, methodName: String, vararg args: Any?): T? {
        return try {
            val method = clazz.getMethod(
                methodName,
                *args.map { it?.javaClass ?: Any::class.java }.toTypedArray()
            )
            method.isAccessible = true
            method.invoke(null) as? T
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 获取与此name与参数想匹配的Method实例
     */
    fun <T> getMethod(name: String, vararg types: Class<*>): MethodReflect<T>? {
        var find: Method? = null
        var type: Class<*>? = clazz
        while (type != null) {
            try {
                find = type.getDeclaredMethod(name, *types)
                break
            } catch (e: NoSuchMethodException) {
                for (method in type.declaredMethods) {
                    if (method.name == name && match(method.parameterTypes, types)) {
                        find = method
                        break
                    }
                }
                // 如果找不到尝试纯粹匹配方法名找到第一个
                if (find == null) {
                    for (method in type.declaredMethods) {
                        if (method.name == name) {
                            find = method
                            break
                        }
                    }
                }
                type = type.superclass
            }
        }
        return if (find == null) null else MethodReflect(accessible(find), this)
    }

    /**
     * 获取所有的方法：包括父类的
     */
    fun getMethods(): List<MethodReflect<Any>> {
        val list = mutableListOf<MethodReflect<Any>>()
        var type: Class<*>? = clazz
        do {
            for (method in type!!.declaredMethods) {
                list.add(MethodReflect(accessible(method), this))
            }
            type = type.superclass
        } while (type != null)
        return list
    }

    /**
     * 获取与此EasyReflect相绑定的实例。
     * PS: 由于泛型擦除问题, 在get方法内部无法判断到泛型的实际类型并做try catch处理
     */
    @Throws(ClassCastException::class)
    fun <T> get(): T? {
        if (instance is Unit) {
            return null
        }
        @Suppress("UNCHECKED_CAST")
        return instance as T?
    }

    /**
     * 创建一个与此class相绑定的动态代理
     */
    fun <T> proxy(proxy: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return Proxy.newProxyInstance(proxy.classLoader, arrayOf(proxy)) { _, method, args ->
            try {
                // 优先匹配存在的方法
                return@newProxyInstance this@EasyReflect.callWithReturn(
                    method.name,
                    *args ?: arrayOf()
                )?.get()
            } catch (e: Exception) {
                return@newProxyInstance handleProxyException(method, args)
            }
        } as T
    }

    private fun handleProxyException(method: Method, args: Array<out Any?>?): Any? {
        if (args != null) {
            try {
                val methodName = method.name
                if (methodName == "get" && args.size == 1 && args[0] is String) {
                    return getFieldValue(args[0] as String)
                } else if (methodName == "set" && args.size == 2 && args[0] is String) {
                    setField(args[0] as String, args[1])
                } else if (methodName.startsWith("get") && method.returnType != Void::class.java) {
                    val name = methodName.substring(3, 4).lowercase() + methodName.substring(4)
                    return getFieldValue(name)
                } else if (methodName.startsWith("set") && args.size == 1) {
                    val name = methodName.substring(3, 4).lowercase() + methodName.substring(4)
                    setField(name, args[0])
                }
            } catch (_: Exception) {
            }
        }
        return getMethodDefaultReturnType(method)
    }

    private fun getMethodDefaultReturnType(method: Method): Any? {
        return when (method.returnType.canonicalName) {
            "byte" -> 0.toByte()
            "short" -> 0.toShort()
            "int" -> 0
            "long" -> 0.toLong()
            "float" -> 0.toFloat()
            "double" -> 0.toDouble()
            "char" -> '0'
            "boolean" -> false
            else -> null
        }
    }

    /**
     * 返回反射类型 T 对应的 Array<T> 类型, 用于部分需要指定 cls 参数获取构造函数的场景
     */
    fun arrayClass(): Class<*> {
        return arrayCls(clazz)
    }

    // 检查是否存在有效的可操作实例。若不存在则抛出异常。
    private fun checkInstance(obj: Any? = instance) {
        if (obj != null) {
            return
        }
        throw ReflectException("Reflect instance of [${clazz.canonicalName}] not found")
    }

    override fun toString(): String {
        return "EasyReflect(clazz=$clazz, instance=$instance)"
    }

    companion object {
        private const val TAG = "EasyReflect"

        @JvmStatic
        fun create(clazz: Class<*>, any: Any? = null): EasyReflect {
            return EasyReflect(clazz, any)
        }

        @JvmStatic
        fun create(any: Any): EasyReflect {
            return when (any) {
                is Class<*> -> create(any)
                is String -> create(any)
                else -> create(any.javaClass, any)
            }
        }

        @JvmStatic
        fun create(name: String, loader: ClassLoader? = null): EasyReflect {
            return try {
                if (loader == null) {
                    create(Class.forName(name))
                } else {
                    create(Class.forName(name, true, loader))
                }
            } catch (e: Exception) {
                EasyReflect(Unit::class.java, name)
            }
        }

        @JvmStatic
        fun types(vararg args: Any?): Array<Class<*>> {
            if (args.isEmpty()) {
                return arrayOf()
            }
            return Array(args.size) { index -> args[index]?.javaClass ?: Void::class.java }
        }

        @JvmStatic
        fun <T : AccessibleObject> accessible(accessible: T): T {
            if (!accessible.isAccessible) {
                accessible.isAccessible = true
            }
            return accessible
        }

        @JvmStatic
        fun match(declaredTypes: Array<out Class<*>>, actualTypes: Array<out Class<*>>): Boolean {
            if (declaredTypes.size != actualTypes.size) return false
            for ((index, declared) in declaredTypes.withIndex()) {
                val actualType = actualTypes[index]
                if ((actualType == Void::class.java && !declared.isPrimitive) ||
                    box(declared).isAssignableFrom(box(actualTypes[index]))
                ) {
                    continue
                }
                return false
            }

            return true
        }

        @JvmStatic
        fun arrayCls(clazz: Class<*>): Class<*> {
            return java.lang.reflect.Array.newInstance(clazz, 0).javaClass
        }

        @JvmStatic
        fun box(source: Class<*>): Class<*> = when (source.name) {
            // 把java基础类型的对象类型统一转成Kt基础类型
            "java.lang.Byte" -> Byte::class.java
            "java.lang.Short" -> Short::class.java
            "java.lang.Integer" -> Int::class.java
            "java.lang.Long" -> Long::class.java
            "java.lang.Float" -> Float::class.java
            "java.lang.Double" -> Double::class.java
            "java.lang.Boolean" -> Boolean::class.java
            "java.lang.Character" -> Char::class.java
            "void" -> Unit::class.java
            else -> source
        }
    }

    class ConstructorReflect<T>(
        val constructor: Constructor<*>,
        @Suppress("unused") val upper: EasyReflect
    ) {
        // 参数是否为可变参数
        fun createReflect(vararg args: Any?): EasyReflect {
            return create(constructor.newInstance(*args))
        }

        @Suppress("UNCHECKED_CAST")
        fun newInstance(vararg args: Any?): T {
            return constructor.newInstance(*args) as T
        }
    }

    /**
     * 静态成员方法反射操作类,该类仅用于操作静态方法
     */
    class StaticMethodReflect<T>(val method: Method, private val upper: EasyReflect) {
        @Suppress("UNCHECKED_CAST")
        fun call(vararg args: Any?): T? {
            return try {
                method.invoke(upper.clazz, *args) as T
            } catch (e: Exception) {
                Log.w(TAG, "Failed to call static method: ${method.name}", e)
                null
            }
        }

        @Suppress("UNCHECKED_CAST")
        fun callNotCatch(vararg args: Any?): T? {
            return try {
                method.invoke(upper.clazz, *args) as T
            } catch (e: Exception) {
                Log.w(TAG, "Failed to call static method: ${method.name}", e)
                throw e
            }
        }
    }

    // 成员方法反射操作类
    class MethodReflect<T>(val method: Method, val upper: EasyReflect) {
        val isStatic = Modifier.isStatic(method.modifiers)

        @Suppress("UNCHECKED_CAST")
        fun callStatic(vararg args: Any?): T? {
            return call(null, *args) as T
        }

        @Suppress("UNCHECKED_CAST")
        fun call(obj: Any? = upper.instance, vararg args: Any?): T? {
            return try {
                if (isStatic) {
                    method.invoke(upper.clazz, *args) as T
                } else {
                    upper.checkInstance(obj)
                    method.invoke(obj, *args) as T
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to call method: ${method.name}", e)
                null
            }
        }

        @Suppress("UNCHECKED_CAST")
        fun callNotCatch(obj: Any? = upper.instance, vararg args: Any?): T? {
            return try {
                if (isStatic) {
                    method.invoke(upper.clazz, *args) as T
                } else {
                    upper.checkInstance(obj)
                    method.invoke(obj, *args) as T
                }
            } catch (e: InvocationTargetException) {
                Log.w(TAG, "Failed to call method: ${method.name}", e)
                throw e.targetException
            } catch (e: Exception) {
                Log.w(TAG, "Failed to call method: ${method.name}", e)
                null
            }
        }

        fun callReflect(vararg args: Any?): MethodReflect<T> {
            if (isStatic) {
                method.invoke(upper.clazz, *args)
            } else {
                upper.checkInstance()
                method.invoke(upper.instance, *args)
            }
            return this
        }

        fun callWithReturn(vararg args: Any?): EasyReflect {
            val value = if (isStatic) {
                method.invoke(upper.clazz, *args)
            } else {
                upper.checkInstance()
                method.invoke(upper.instance, *args)
            }

            return create(value ?: method.returnType)
        }
    }

    /**
     * 静态成员变量反射操作类, 该类只用于操作静态成员变量
     */
    class StaticFieldReflect<T>(val field: Field, private val upper: EasyReflect) {

        val type: Class<*> = field.type

        @Suppress("UNCHECKED_CAST")
        fun getValue(): T? {
            return try {
                field.get(upper.clazz) as T
            } catch (e: Exception) {
                Log.w(TAG, "Failed to get static field: ${field.name}", e)
                null
            }
        }

        fun setValue(value: T?): StaticFieldReflect<T> {
            try {
                field.set(upper.clazz, value)
            } catch (e: Exception) {
                Log.e(TAG, "setValue excepiton:$e")
            }
            return this
        }
    }

    // 成员变量反射操作类
    class FieldReflect<T>(val field: Field, private val upper: EasyReflect) {
        val isStatic = Modifier.isStatic(field.modifiers)

        @Suppress("UNCHECKED_CAST")
        fun getValue(obj: Any? = upper.instance): T? {
            return try {
                if (isStatic) {
                    field.get(upper.clazz) as T
                } else {
                    upper.checkInstance(obj)
                    field.get(obj) as T
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get field: ${field.name}", e)
                null
            }
        }

        fun setValue(obj: Any?, value: T?): FieldReflect<T> {
            try {
                field.set(obj, value)
            } catch (e: Exception) {
                Log.e(TAG, "setValue excepiton:$e")
            }
            return this
        }

        fun setValue(value: T?): FieldReflect<T> {
            try {
                if (isStatic) {
                    field.set(upper.clazz, value)
                } else {
                    upper.checkInstance()
                    field.set(upper.instance, value)
                }
            } catch (e: Exception) {
                Log.e(TAG, "setValue excepiton:$e")
            }
            return this
        }

        @Suppress("unused")
        fun transform(): EasyReflect {
            val value = if (isStatic) {
                field.get(upper.clazz)
            } else {
                upper.checkInstance()
                field.get(upper.instance)
            }
            return create(value ?: field.type)
        }
    }
}

/**
 * 用于在进行反射操作过程中对受检异常错误进行包装
 */
class ReflectException(message: String) : RuntimeException(message)
