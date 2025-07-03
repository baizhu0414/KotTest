package com.example.daggeruse

class Memory constructor() {
    val size: Int = 1024
}

interface Cpu { val brand: String }
class IntelCpu : Cpu { override val brand = "Intel" }

//@Inject constructor 告知 Dagger 可以使用这个构造函数来创建 Computer 实例。
class Computer constructor(private val memory: Memory,private val cpu: Cpu) {
    fun info():String {
        return "Memory: ${memory.size}GB, CPU: ${cpu.brand}"
    }

    fun test() = "Hello${memory.size}GB"

}

