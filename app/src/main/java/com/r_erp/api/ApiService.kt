package com.r_erp.api

import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

data class AgendaItem(
    val title: String,
    val description: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val fullDay: Boolean = false,
)

interface ApiService {
    // Agenda
    @GET("exec")
    suspend fun getAgenda(
        @Query("option") option: String = "agenda",
        @Query("date") date: String,
        @Query("calendarName") calendarName: String
    ): List<AgendaItem>

    @POST("exec")
    suspend fun addAgendaItem(
        @Query("option") option: String = "agenda",
        @Query("calendarName") calendarName: String,
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
