# Implementation Plan - Add Product to Budget

This plan adds a feature to the Products screen allowing users to long-press a product and add it to an existing "electible" budget.

## Proposed Changes

### [Component Name] API Layer

#### [MODIFY] [SupabaseService.kt](file:///C:/Users/borba/AndroidStudioProjects/r-erp/app/src/main/java/com/r_erp/api/SupabaseService.kt)
- Add `SupabaseElectibleBudget` data class:
  ```kotlin
  data class SupabaseElectibleBudget(
      val id: Int? = null,
      @SerializedName("client_name") val clientName: String? = null,
      @SerializedName("created_at") val createdAt: String? = null
  )
  ```
- Add `getElectibleBudgets()` method to `SupabaseService` interface:
  ```kotlin
  @GET("electible_budgets")
  suspend fun getElectibleBudgets(): List<SupabaseElectibleBudget>
  ```
- Add a single-item `createBudgetItem` method to `SupabaseService` interface:
  ```kotlin
  @POST("budget_items")
  suspend fun createBudgetItem(@Body item: SupabaseBudgetItemRequest): Response<Unit>
  ```

### [Component Name] UI Layer

#### [MODIFY] [ProductsScreen.kt](file:///C:/Users/borba/AndroidStudioProjects/r-erp/app/src/main/java/com/r_erp/ui/screens/ProductsScreen.kt)
- Update `ProductItem` to include a new "Adicionar ao orçamento ..." item in the long-press `DropdownMenu`.
- Add state variables to `ProductsScreen` to manage the "Add to Budget" dialog:
    - `showAddToBudgetDialog: Boolean`
    - `productToAddToBudget: SupabaseProduct?`
- Implement `AddToBudgetDialog` composable:
    - Fetches electible budgets using `supabaseService.getElectibleBudgets()` on launch.
    - Displays a filterable `ExposedDropdownMenuBox` (reusing the logic from previous improvements) to select a budget.
    - Includes an `OutlinedTextField` for quantity (numeric keyboard).
    - Includes "Adicionar" and "Cancelar" buttons.
    - On "Adicionar":
        - POSTs to `budget_items` with `budget_id`, `product_id`, `price` (from product), and `quantity`.
        - Shows a success message (Toast or Snackbar) and closes the dialog on success.
        - Shows an error message and keeps the dialog open on failure.

## Verification Plan

### Automated Tests
- None requested, but manual verification will be thorough.

### Manual Verification
1.  Open the app and navigate to the Products screen.
2.  Long-press a product.
3.  Verify "Adicionar ao orçamento ..." appears in the menu.
4.  Click "Adicionar ao orçamento ...".
5.  Verify the dialog opens and displays a list of electible budgets in the combo box.
6.  Enter a quantity.
7.  Select a budget.
8.  Click "Adicionar".
9.  Verify the success message appears and the dialog closes.
10. Check if the item was actually added to the budget (optional but recommended if budget details screen is accessible).
11. Test the "Cancelar" button to ensure it closes the dialog without action.
12. Test error handling (e.g., trying to add without selecting a budget or quantity).
