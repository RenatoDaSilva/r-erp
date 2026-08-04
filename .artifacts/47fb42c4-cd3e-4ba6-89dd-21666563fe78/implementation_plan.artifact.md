# Implementation Plan - Add Product to Order

This plan adds a feature to the Products screen allowing users to long-press a product and add it to an existing "electible" order.

## Proposed Changes

### [Component Name] API Layer

#### [MODIFY] [SupabaseService.kt](file:///C:/Users/borba/AndroidStudioProjects/r-erp/app/src/main/java/com/r_erp/api/SupabaseService.kt)
- Add `SupabaseElectibleOrder` data class:
  ```kotlin
  data class SupabaseElectibleOrder(
      val id: Int? = null,
      @SerializedName("client_name") val clientName: String? = null,
      @SerializedName("created_at") val createdAt: String? = null
  )
  ```
- Add `getElectibleOrders()` method to `SupabaseService` interface:
  ```kotlin
  @GET("electible_orders")
  suspend fun getElectibleOrders(): List<SupabaseElectibleOrder>
  ```
- Add a single-item `createOrderItem` method to `SupabaseService` interface:
  ```kotlin
  @POST("order_items")
  suspend fun createOrderItem(@Body item: SupabaseOrderItemRequest): Response<Unit>
  ```

### [Component Name] UI Layer

#### [MODIFY] [ProductsScreen.kt](file:///C:/Users/borba/AndroidStudioProjects/r-erp/app/src/main/java/com/r_erp/ui/screens/ProductsScreen.kt)
- Update `ProductItem` to include a new "Adicionar ao pedido ..." item in the long-press `DropdownMenu`.
- Add state variables to `ProductsScreen` to manage the "Add to Order" dialog:
    - `showAddToOrderDialog: Boolean`
    - `productToAddToOrder: SupabaseProduct?`
- Implement `AddToOrderDialog` composable:
    - Fetches electible orders using `supabaseService.getElectibleOrders()` on launch.
    - Displays a filterable `ExposedDropdownMenuBox` to select an order.
    - Includes an `OutlinedTextField` for quantity (numeric keyboard).
    - Includes "Adicionar" and "Cancelar" buttons.
    - On "Adicionar":
        - POSTs to `order_items` with `order_id`, `product_id`, `price` (from product), and `quantity`.
        - Shows a success message (Toast) and closes the dialog on success.
        - Shows an error message and keeps the dialog open on failure.

## Verification Plan

### Manual Verification
1.  Open the app and navigate to the Products screen.
2.  Long-press a product.
3.  Verify "Adicionar ao pedido ..." appears in the menu.
4.  Click "Adicionar ao pedido ...".
5.  Verify the dialog opens and displays a list of electible orders in the combo box.
6.  Enter a quantity.
7.  Select an order.
8.  Click "Adicionar".
9.  Verify the success message "Produto adicionado ao pedido [id] com sucesso" appears and the dialog closes.
10. Test the "Cancelar" button to ensure it closes the dialog without action.
