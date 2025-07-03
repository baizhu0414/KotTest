package com.example.daggeruse

import com.example.kottest.MainActivity
import dagger.Component

@Component(modules = [ComputerModule::class])
public interface ComputerComponent {
    //
    /*
    * 参数：  表示需要注入的目标。扫描MainActivity中@Inject注解的变量。
    * 返回值：Dagger 会依据 @Component 注解里指定的模块（这里是 CpuModule::class，@Provides 注解的方法），
    *        以及项目中其他带有 @Inject 注解的构造函数，
    *        来创建并返回实例（有返回值时）。
    * */
//    Dagger1111
//    fun injectActivity(activity: MainActivity)
}