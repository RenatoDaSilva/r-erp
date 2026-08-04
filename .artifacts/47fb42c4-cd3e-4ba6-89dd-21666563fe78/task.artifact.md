# Tasks - Add Product to Budget

- [x] Update `SupabaseService.kt`
    - [x] Add `SupabaseElectibleBudget` data class
    - [x] Add `getElectibleBudgets` and `createBudgetItem` to `SupabaseService` interface
- [x] Update `ProductsScreen.kt`
    - [x] Add "Adicionar ao orçamento ..." to `ProductItem` long-press menu
    - [x] Add state for "Add to Budget" dialog
    - [x] Implement `AddToBudgetDialog` composable
    - [x] Handle "Adicionar" action (API call, success/error messages)
- [x] Verify the implementation
