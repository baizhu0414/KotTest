package com.example.daggeruse

import android.util.Log
import dagger.Module
import dagger.Provides

@Module
public class ComputerModule {
    @Provides
    fun provideMemory():Memory {
        Log.d("11111Dagger-provideMemory","Computer Memory")
        return Memory()
    }

    @Provides
    fun provideCpu():Cpu {
        Log.d("11111Dagger-provideCpu","Computer Cpu")
        return IntelCpu()
    }

    @Provides
    fun provideComputer(memory:Memory,cpu:Cpu):Computer {
        Log.d("11111Dagger-provideComputer","Computer")
        return Computer(memory,cpu)
    }
}