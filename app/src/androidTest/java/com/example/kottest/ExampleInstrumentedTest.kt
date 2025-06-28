package com.example.kottest

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import kotlin.math.log

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun main() {
        var a = 1
        a = 100
        val b = 1
//        b = 100

        val muList = mutableListOf(1,2,3)
        muList.add(4)
        val immuList = listOf(1,2,3)
//        immuList.add(9)
    }


}