package com.r_erp.api

import com.r_erp.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

interface SupabaseService {

    // Adicione os métodos da API aqui futuramente

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
