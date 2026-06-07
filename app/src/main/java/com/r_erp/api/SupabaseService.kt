package com.r_erp.api

import com.r_erp.BuildConfig
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import com.r_erp.utils.SessionManager
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Route
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query
import androidx.compose.runtime.compositionLocalOf

val LocalToken = compositionLocalOf { "" }
val LocalSessionManager = compositionLocalOf<SessionManager?> { null }

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
    @SerializedName("generates_stock") val generatesStock: Boolean? = null,
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
    val id: Int? = null,
    @SerializedName("budget_id") val budgetId: Int? = null,
    @SerializedName("product_id") val productId: Int? = null,
    @SerializedName("service_id") val serviceId: Int? = null,
    val description: String? = null,
    val quantity: Double? = null,
    val price: Double? = null,
    val discount: Double? = null,
    val total: Double? = null
)

data class SupabaseOrderItem(
    val id: Int? = null,
    @SerializedName("order_id") val orderId: Int? = null,
    @SerializedName("product_id") val productId: Int? = null,
    @SerializedName("service_id") val serviceId: Int? = null,
    val description: String? = null,
    val quantity: Double? = null,
    val price: Double? = null,
    val discount: Double? = null,
    val total: Double? = null
)

data class SupabasePurchaseItem(
    val id: Int? = null,
    @SerializedName("purchase_id") val purchaseId: Int? = null,
    @SerializedName("product_id") val productId: Int? = null,
    val description: String? = null,
    @SerializedName("product") val product: ProductNested? = null,
    val quantity: Double? = null,
    val price: Double? = null,
    val discount: Double? = null,
    val total: Double? = null
)

data class ProductNested(
    val description: String? = null
)

// Specific DTO for inserting items
data class SupabaseBudgetItemRequest(
    @SerializedName("budget_id") val budgetId: Int?,
    @SerializedName("product_id") val productId: Int?,
    @SerializedName("service_id") val serviceId: Int?,
    val quantity: Double?,
    val price: Double?,
    val discount: Double?
)

data class SupabaseOrderItemRequest(
    @SerializedName("order_id") val orderId: Int?,
    @SerializedName("product_id") val productId: Int?,
    @SerializedName("service_id") val serviceId: Int?,
    val quantity: Double?,
    val price: Double?,
    val discount: Double?
)

data class SupabasePurchaseItemRequest(
    @SerializedName("purchase_id") val purchaseId: Int?,
    @SerializedName("product_id") val productId: Int?,
    val quantity: Double?,
    val price: Double?,
    val discount: Double?
)

data class SupabaseBudget(
    val id: Int? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("client_id") val clientId: Int? = null,
    @SerializedName("client_name") val clientName: String? = null,
    val phone: String? = null,
    val city: String? = null,
    val state: String? = null,
    @SerializedName("valid_until") val validUntil: String? = null,
    @SerializedName("total_items") val totalItems: Double? = null,
    val discount: Double? = null,
    val total: Double? = null,
    val message: String? = null,
    @SerializedName("order_id") val orderId: Int? = null,
    @SerializedName("items_count") val itemsCount: Int? = null,
    val items: List<SupabaseBudgetItem>? = null
)

data class SupabaseOrder(
    val id: Int? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("client_id") val clientId: Int? = null,
    @SerializedName("client_name") val clientName: String? = null,
    val phone: String? = null,
    val city: String? = null,
    val state: String? = null,
    @SerializedName("total_items") val totalItems: Double? = null,
    val discount: Double? = null,
    val total: Double? = null,
    val message: String? = null,
    @SerializedName("budget_id") val budgetId: Int? = null,
    @SerializedName("items_count") val itemsCount: Int? = null,
    val items: List<SupabaseOrderItem>? = null
)

data class SupabasePurchase(
    val id: Int? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("supplier_id") val supplierId: Int? = null,
    @SerializedName("supplier_name") val supplierName: String? = null,
    @SerializedName("pay_until") val payUntil: String? = null,
    val discount: Double? = null,
    val freight: Double? = null,
    val tax: Double? = null,
    val total: Double? = null,
    val message: String? = null,
    @SerializedName("items_count") val itemsCount: Int? = null,
    val items: List<SupabasePurchaseItem>? = null
)

data class SupabaseReceivable(
    val id: Int? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("client_id") val clientId: Int? = null,
    @SerializedName("client_fullname") val clientFullName: String? = null,
    val origin: String? = null,
    @SerializedName("order_id") val orderId: Int? = null,
    val value: Double? = null,
    @SerializedName("due_date") val dueDate: String? = null,
    @SerializedName("paid_at") val paidAt: String? = null,
    @SerializedName("paid_value") val paidValue: Double? = null
)

data class SupabaseReceivableTotal(
    val outstanding: Double?,
    val paid: Double?
)

data class SupabaseConfig(
    val id: Int? = null,
    @SerializedName("user_id") val userId: String? = null,
    val logo: String? = null,
    @SerializedName("company_name") val companyName: String? = null,
    @SerializedName("company_address") val companyAddress: String? = null,
    @SerializedName("company_phone") val companyPhone: String? = null,
    @SerializedName("cnpj_cpf") val cnpjCpf: String? = null,
    @SerializedName("default_message_budget") val defaultMessageBudget: String? = null,
    @SerializedName("default_message_order") val defaultMessageOrder: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

data class AuthResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String? = null,
    val user: SupabaseUser
)

data class SupabaseUser(
    val id: String,
    val email: String
)

interface SupabaseService {

    @POST("https://euzmbicrbjpgcyrojvdm.supabase.co/auth/v1/signup")
    suspend fun signUp(@Body body: Map<String, String>): Response<AuthResponse>

    @POST("https://euzmbicrbjpgcyrojvdm.supabase.co/auth/v1/token?grant_type=password")
    suspend fun signIn(@Body body: Map<String, String>): Response<AuthResponse>

    @POST("https://euzmbicrbjpgcyrojvdm.supabase.co/auth/v1/token?grant_type=refresh_token")
    suspend fun refreshToken(@Body body: Map<String, String>): Response<AuthResponse>

    @GET("budgets_with_items")
    suspend fun getBudgetsWithItems(): List<SupabaseBudget>

    @GET("budgets_with_items")
    suspend fun getBudgetWithItems(@Query("id") idFilter: String): List<SupabaseBudget>

    @POST("budgets")
    suspend fun createBudget(@Body budget: Map<String, @JvmSuppressWildcards Any>): Response<Unit>

    @PATCH("budgets")
    suspend fun updateBudget(
        @Query("id") idFilter: String,
        @Body budget: Map<String, @JvmSuppressWildcards Any>
    ): Response<Unit>

    @POST("budget_items")
    suspend fun createBudgetItems(@Body items: List<SupabaseBudgetItemRequest>): Response<Unit>

    @GET("budget_items")
    suspend fun getBudgetItems(@Query("budget_id") budgetIdFilter: String): List<SupabaseBudgetItem>

    @DELETE("budget_items")
    suspend fun deleteBudgetItems(@Query("budget_id") budgetIdFilter: String): Response<Unit>

    @GET("orders_with_items")
    suspend fun getOrdersWithItems(): List<SupabaseOrder>

    @GET("orders_with_items")
    suspend fun getOrderWithItems(@Query("id") idFilter: String): List<SupabaseOrder>

    @POST("orders")
    suspend fun createOrder(@Body order: Map<String, @JvmSuppressWildcards Any>): Response<Unit>

    @PATCH("orders")
    suspend fun updateOrder(
        @Query("id") idFilter: String,
        @Body order: Map<String, @JvmSuppressWildcards Any>
    ): Response<Unit>

    @POST("order_items")
    suspend fun createOrderItems(@Body items: List<SupabaseOrderItemRequest>): Response<Unit>

    @GET("order_items")
    suspend fun getOrderItems(@Query("order_id") orderIdFilter: String): List<SupabaseOrderItem>

    @DELETE("order_items")
    suspend fun deleteOrderItems(@Query("order_id") orderIdFilter: String): Response<Unit>

    @GET("purchases")
    suspend fun getPurchasesWithItems(
        @Query("select") select: String = "*,items:purchase_items(*,product:products(description))"
    ): List<SupabasePurchase>

    @GET("purchases")
    suspend fun getPurchaseWithItems(
        @Query("id") idFilter: String,
        @Query("select") select: String = "*,items:purchase_items(*,product:products(description))"
    ): List<SupabasePurchase>

    @POST("purchases")
    suspend fun createPurchase(@Body purchase: Map<String, @JvmSuppressWildcards Any>): Response<Unit>

    @PATCH("purchases")
    suspend fun updatePurchase(
        @Query("id") idFilter: String,
        @Body purchase: Map<String, @JvmSuppressWildcards Any>
    ): Response<Unit>

    @POST("purchase_items")
    suspend fun createPurchaseItems(@Body items: List<SupabasePurchaseItemRequest>): Response<Unit>

    @GET("purchase_items")
    suspend fun getPurchaseItems(@Query("purchase_id") purchaseIdFilter: String): List<SupabasePurchaseItem>

    @DELETE("purchase_items")
    suspend fun deletePurchaseItems(@Query("purchase_id") purchaseIdFilter: String): Response<Unit>

    @GET("receivables")
    suspend fun getReceivables(): List<SupabaseReceivable>

    @GET("receivables")
    suspend fun getReceivable(@Query("id") idFilter: String): List<SupabaseReceivable>

    @POST("receivables")
    suspend fun createReceivable(@Body receivable: Map<String, @JvmSuppressWildcards Any>): Response<Unit>

    @PATCH("receivables")
    suspend fun updateReceivable(
        @Query("id") idFilter: String,
        @Body receivable: Map<String, @JvmSuppressWildcards Any>
    ): Response<Unit>

    @GET("receivables_totals")
    suspend fun getReceivablesTotals(): List<SupabaseReceivableTotal>

    @GET("config")
    suspend fun getConfig(): List<SupabaseConfig>

    @POST("config")
    suspend fun createConfig(@Body config: Map<String, @JvmSuppressWildcards Any>): Response<Unit>

    @PATCH("config")
    suspend fun updateConfig(
        @Query("id") idFilter: String,
        @Body config: Map<String, @JvmSuppressWildcards Any>
    ): Response<Unit>

    @GET("services")
    suspend fun getServices(): List<SupabaseServiceItem>

    @GET("services")
    suspend fun getService(@Query("id") idFilter: String): List<SupabaseServiceItem>

    @POST("services")
    suspend fun createService(@Body service: Map<String, @JvmSuppressWildcards Any>): Response<Unit>

    @PATCH("services")
    suspend fun updateService(
        @Query("id") idFilter: String,
        @Body service: Map<String, @JvmSuppressWildcards Any>
    ): Response<Unit>

    @GET("product_types")
    suspend fun getProductTypes(): List<SupabaseType>

    @GET("product_units")
    suspend fun getProductUnits(): List<SupabaseUnit>

    @GET("products")
    suspend fun getProducts(): List<SupabaseProduct>

    @GET("products")
    suspend fun getProduct(@Query("id") idFilter: String): List<SupabaseProduct>

    @POST("products")
    suspend fun createProduct(@Body product: Map<String, @JvmSuppressWildcards Any>): Response<Unit>

    @PATCH("products")
    suspend fun updateProduct(
        @Query("id") idFilter: String,
        @Body product: Map<String, @JvmSuppressWildcards Any>
    ): Response<Unit>

    @GET("clients")
    suspend fun getClients(): List<SupabaseClient>

    @GET("clients")
    suspend fun getClient(@Query("id") idFilter: String): List<SupabaseClient>

    @GET("suppliers")
    suspend fun getSuppliers(): List<SupabaseSupplier>

    @GET("suppliers")
    suspend fun getSupplier(@Query("id") idFilter: String): List<SupabaseSupplier>

    @POST("suppliers")
    suspend fun createSupplier(@Body supplier: Map<String, @JvmSuppressWildcards Any>): Response<Unit>

    @PATCH("suppliers")
    suspend fun updateSupplier(
        @Query("id") idFilter: String,
        @Body supplier: Map<String, @JvmSuppressWildcards Any>
    ): Response<Unit>

    @DELETE("suppliers")
    suspend fun deleteSupplier(@Query("id") idFilter: String): Response<Unit>

    @POST("clients")
    suspend fun createClient(@Body client: Map<String, @JvmSuppressWildcards Any>): Response<Unit>

    @PATCH("clients")
    suspend fun updateClient(
        @Query("id") idFilter: String,
        @Body client: Map<String, @JvmSuppressWildcards Any>
    ): Response<Unit>

    @DELETE("clients")
    suspend fun deleteClient(@Query("id") idFilter: String): Response<Unit>

    @POST("rpc/get_sequence")
    suspend fun getSequence(@Body body: Map<String, String>): Int

    @POST("rpc/create_order_from_budget")
    suspend fun createOrderFromBudget(@Body body: Map<String, Int>): Int

    @POST("rpc/build_purchase")
    suspend fun buildPurchase(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<Unit>

    @POST("rpc/adjust_stock")
    suspend fun adjustStock(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<Unit>

    companion object {
        private const val BASE_URL = "https://euzmbicrbjpgcyrojvdm.supabase.co/rest/v1/"
        private const val API_KEY = BuildConfig.SUPABASE_KEY
        
        var currentToken: String? = null

        fun create(token: String? = null, sessionManager: SessionManager? = null): SupabaseService {
            val authInterceptor = Interceptor { chain ->
                val requestBuilder = chain.request().newBuilder()
                    .header("Content-Type", "application/json")
                    .header("apikey", API_KEY)
                    .header("X-Client-Info", "r-erp-android")
                
                val finalToken: String = when {
                    !token.isNullOrBlank() -> token
                    !currentToken.isNullOrBlank() -> currentToken!!
                    else -> API_KEY
                }

                requestBuilder.header("Authorization", "Bearer $finalToken")
                
                val request = requestBuilder.build()
                chain.proceed(request)
            }

            val authenticator = object : Authenticator {
                override fun authenticate(route: Route?, response: okhttp3.Response): Request? {
                    if (sessionManager == null) return null
                    
                    // Only try to refresh once
                    if (response.priorResponse != null) return null

                    return runBlocking {
                        val refreshToken = sessionManager.getRefreshToken()
                        if (refreshToken != null) {
                            try {
                                // Create a separate service instance for refresh to avoid loops
                                val refreshService = create() 
                                val refreshResponse = refreshService.refreshToken(mapOf("refresh_token" to refreshToken))
                                
                                val newToken = refreshResponse.body()?.accessToken
                                if (newToken != null) {
                                    sessionManager.updateAccessToken(newToken)
                                    currentToken = newToken
                                    
                                    response.request.newBuilder()
                                        .header("Authorization", "Bearer $newToken")
                                        .build()
                                } else {
                                    null
                                }
                            } catch (e: Exception) {
                                null
                            }
                        } else {
                            null
                        }
                    }
                }
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .authenticator(authenticator)
                .build()

            val gson = GsonBuilder()
                .serializeNulls()
                .create()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()

            return retrofit.create(SupabaseService::class.java)
        }

        fun getUserIdFromToken(token: String): String? {
            return try {
                val parts = token.split(".")
                if (parts.size < 2) return null
                val payload = String(android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE))
                val jsonObject = com.google.gson.JsonParser.parseString(payload).asJsonObject
                jsonObject.get("sub")?.asString
            } catch (e: Exception) {
                null
            }
        }
    }
}
