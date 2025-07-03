package com.example.daggeruse.network

import retrofit2.http.GET

/**
 * Retrofit API 接口
 */
interface ApiService {
    @GET("users")
    suspend fun getUsers(): List<UserEntity>
}
