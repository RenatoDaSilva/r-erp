package com.r_erp.api

import com.r_erp.BuildConfig
import com.google.gson.annotations.SerializedName
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

data class SupabaseClient(
    val id: Int? = null,
    @SerializedName("fullname") val fullName: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val city: String? = null,
    val state: String? = null,
    val cpf: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

interface SupabaseService {

    @GET("clients")
    suspend fun getClients(): List<SupabaseClient>

    @GET("clients")
    suspend fun getClient(@Query("id") idFilter: String): List<SupabaseClient>

    @POST("clients")
    suspend fun createClient(@Body client: SupabaseClient): ResponseBody

    @PATCH("clients")
    suspend fun updateClient(
        @Query("id") idFilter: String,
        @Body client: SupabaseClient
    ): ResponseBody

    companion object {
        private const val BASE_URL = "https://euzmbicrbjpgcyrojvdm.supabase.co/rest/v1/"
        private const val API_KEY = BuildConfig.SUPABASE_KEY

        fun create(): SupabaseService {
            val authInterceptor = Interceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Content-Type", "application/json")
                    .addHeader("apikey", API_KEY)
                    .addHeader("Authorization", "Bearer $API_KEY")
                    .build()
                chain.proceed(request)
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return retrofit.create(SupabaseService::class.java)
        }
    }
}
