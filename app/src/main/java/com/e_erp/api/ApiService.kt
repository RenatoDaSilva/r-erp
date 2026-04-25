package com.r_erp.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

data class ApiResponse(
    val message: String,
    val queryParams: Map<String, String>,
)

interface ApiService {
    @GET("exec")
    suspend fun getData(): ApiResponse

    companion object {
        private const val BASE_URL = "https://script.google.com/macros/s/AKfycbyMfVdXPP28YqpxfgXSNlBjlnaSD-ltURq8A7TTLFlVIbhVGh43Y1qJtKV5lASf1t23/"

        fun create(): ApiService {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return retrofit.create(ApiService::class.java)
        }
    }
}
