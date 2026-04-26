package com.r_erp.api

import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
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

data class Supplier(
    val id: Int,
    val fullname: String,
    val phone: String,
    val email: String? = null,
    val address: String? = null,
    val city: String? = null,
    val state: String? = null,
    val cpf: String? = null, // Can be CNPJ
    val date: String? = null,
)

data class AgendaItem(
    val title: String,
    val description: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val fullDay: Boolean = false,
)

interface ApiService {
    // Clients
    @GET("exec")
    suspend fun getClients(@Query("option") option: String = "clientes"): List<Client>

    @GET("exec")
    suspend fun getClient(
        @Query("option") option: String = "cliente",
        @Query("id") id: Int
    ): Client

    @POST("exec")
    suspend fun updateClient(
        @Query("option") option: String = "cliente",
        @Body client: Client
    ): ResponseBody

    // Suppliers
    @GET("exec")
    suspend fun getSuppliers(@Query("option") option: String = "fornecedores"): List<Supplier>

    @GET("exec")
    suspend fun getSupplier(
        @Query("option") option: String = "fornecedor",
        @Query("id") id: Int
    ): Supplier

    @POST("exec")
    suspend fun updateSupplier(
        @Query("option") option: String = "fornecedor",
        @Body supplier: Supplier
    ): ResponseBody

    // Agenda
    @GET("exec")
    suspend fun getAgenda(
        @Query("option") option: String = "agenda",
        @Query("date") date: String
    ): List<AgendaItem>

    @POST("exec")
    suspend fun addAgendaItem(
        @Query("option") option: String = "agenda",
        @Body item: AgendaItem
    ): ResponseBody

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
