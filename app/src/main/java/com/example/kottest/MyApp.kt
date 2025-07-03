package com.example.kottest

import android.app.Application
import com.example.daggeruse.network.DaggerNetworkDatabaseComponent
import com.example.daggeruse.network.DatabaseModule

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        DaggerNetworkDatabaseComponent.builder()
            .context(this)
            .dbName("user_db.db")
//            .databaseModule(DatabaseModule(this, "user_db.db"))
            .build()
    }
}
