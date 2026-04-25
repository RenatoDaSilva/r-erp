package com.r_erp.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

data class Client(
    val id: Int,
    val fullname: String,
    val phone: String,
    val email: String? = null,
    val address: String? = null,
    val city: String? = null,
    val state: String? = null,
    val cpf: String? = null,
    val date: String? = null,
)

interface ApiService {
    @GET("exec")
    suspend fun getClients(@Query("option") option: String = "clientes"): List<Client>

    @GET("exec")
    suspend fun getClient(
        @Query("option") option: String = "cliente",
        @Query("id") id: Int
    ): Client

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
