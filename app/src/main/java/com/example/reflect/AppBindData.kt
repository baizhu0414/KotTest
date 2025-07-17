package com.example.reflect

object AppBindData : Reflect("android.app.ActivityThread\$AppBindData") {
    val providers = getField<List<Any>>("providers")
}