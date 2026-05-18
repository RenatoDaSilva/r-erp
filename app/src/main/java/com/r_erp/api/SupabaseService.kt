package com.r_erp.api

import com.r_erp.BuildConfig
import com.google.gson.annotations.SerializedName
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
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

data class SupabaseSupplier(
    val id: Int? = null,
    @SerializedName("fullname") val fullName: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val city: String? = null,
    val state: String? = null,
    @SerializedName("cpfcnpj") val cpfCnpj: String? = null,
    val pix: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

data class SupabaseProduct(
    val id: Int? = null,
    val description: String? = null,
    val type: String? = null,
    val unit: String? = null,
    val price: Double? = null,
    val stock: Double? = null,
    val cost: Double? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

data class SupabaseUnit(
    val id: Int? = null,
    val unit: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

data class SupabaseType(
    val id: Int? = null,
    val type: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

data class SupabaseServiceItem(
    val id: Int? = null,
    val description: String? = null,
    val price: Double? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

data class SupabaseBudgetItem(
    val description: String? = null,
    val quantity: Double? = null,
    val price: Double? = null,
    val discount: Double? = null,
    val total: Double? = null
)

data class SupabaseBudget(
    val id: Int? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("client_name") val clientName: String? = null,
    @SerializedName("valid_until") val validUntil: String? = null,
    @SerializedName("total_items") val totalItems: Double? = null,
    val discount: Double? = null,
    val total: Double? = null,
    val message: String? = null,
    @SerializedName("order_id") val orderId: Int? = null,
    @SerializedName("items_count") val itemsCount: Int? = null,
    val items: List<SupabaseBudgetItem>? = null
)

interface SupabaseService {

    @GET("budgets_with_items")
    suspend fun getBudgetsWithItems(): List<SupabaseBudget>

    @GET("services")
    suspend fun getServices(): List<SupabaseServiceItem>

    @GET("services")
    suspend fun getService(@Query("id") idFilter: String): List<SupabaseServiceItem>

    @POST("services")
    suspend fun createService(@Body service: SupabaseServiceItem): ResponseBody

    @PATCH("services")
    suspend fun updateService(
        @Query("id") idFilter: String,
        @Body service: SupabaseServiceItem
    ): ResponseBody

    @GET("product_types")
    suspend fun getProductTypes(): List<SupabaseType>

    @GET("product_units")
    suspend fun getProductUnits(): List<SupabaseUnit>

    @GET("products")
    suspend fun getProducts(): List<SupabaseProduct>

    @GET("products")
    suspend fun getProduct(@Query("id") idFilter: String): List<SupabaseProduct>

    @POST("products")
    suspend fun createProduct(@Body product: SupabaseProduct): ResponseBody

    @PATCH("products")
    suspend fun updateProduct(
        @Query("id") idFilter: String,
        @Body product: SupabaseProduct
    ): ResponseBody

    @GET("clients")
    suspend fun getClients(): List<SupabaseClient>

    @GET("clients")
    suspend fun getClient(@Query("id") idFilter: String): List<SupabaseClient>

    @GET("suppliers")
    suspend fun getSuppliers(): List<SupabaseSupplier>

    @GET("suppliers")
    suspend fun getSupplier(@Query("id") idFilter: String): List<SupabaseSupplier>

    @POST("suppliers")
    suspend fun createSupplier(@Body supplier: SupabaseSupplier): ResponseBody

    @PATCH("suppliers")
    suspend fun updateSupplier(
        @Query("id") idFilter: String,
        @Body supplier: SupabaseSupplier
    ): ResponseBody

    @DELETE("suppliers")
    suspend fun deleteSupplier(@Query("id") idFilter: String): ResponseBody

    @POST("clients")
    suspend fun createClient(@Body client: SupabaseClient): ResponseBody

    @PATCH("clients")
    suspend fun updateClient(
        @Query("id") idFilter: String,
        @Body client: SupabaseClient
    ): ResponseBody

    @DELETE("clients")
    suspend fun deleteClient(@Query("id") idFilter: String): ResponseBody

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
