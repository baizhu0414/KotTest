package com.example.daggeruse.network

import android.content.Context
import android.util.Log
import androidx.room.Room
import dagger.Module
import dagger.Provides
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
class NetworkModule {
    @Provides
    @Singleton // 确保单例实例
    fun provideRetrofit(): Retrofit {
        Log.d("Dagger2222","provideRetrofit")
        return Retrofit.Builder()
            .baseUrl("https://jsonplaceholder.typicode.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * provideApiService 方法依赖于 Retrofit 实例。但是不需要通过retrofit手动
     * 调用call方法了。
     */
    @Provides
    fun provideApiService(retrofit: Retrofit): ApiService {
        Log.d("Dagger2222","provideApiService")
        return retrofit.create(ApiService::class.java)
    }
}
@Module
class DatabaseModule() {
//    class DatabaseModule(private val context: Context, private val dbName:String) {
    @Provides
    @Singleton
    fun provideAppDatabase(context:Context, dbName:String): AppDatabase {
        // 参数来自BindsInstance
        return Room.databaseBuilder(context,AppDatabase::class.java,dbName).build()
    }
    @Provides
    fun provideUserDao(db: AppDatabase): UserDao = db.userDao()
}