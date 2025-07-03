package com.example.daggeruse.network

import android.content.Context
import com.example.kottest.MainActivity
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton // 配合Provide的单例，此处必须加注解
@Component(modules = [NetworkModule::class, DatabaseModule::class])
interface NetworkDatabaseComponent {
    fun injectActivity(activity: MainActivity)

    @Component.Builder
    interface Builder {
//        @BindsInstance // 同类型的参数BindsInstance只能有一个。
//        fun baseUrl(baseUrl:String): Builder
        // @BindsInstance 注解的参数会被自动注入到组件的构造函数中
        @BindsInstance
        fun context(context: Context): Builder
        @BindsInstance
        fun dbName(dbName:String): Builder
        /*`DatabaseModule` 构造函数需要参数（如 `Context`）时，必须通过 `Builder` 手动传入实例。
        这是因为 Dagger 无法自动实例化带参模块，需要开发者显式提供模块实例来初始化依赖图。
        此处和上面的BindsInstance注解传构造参数等价。*/
//        fun databaseModule(module: DatabaseModule): Builder
        fun build(): NetworkDatabaseComponent
    }
}
