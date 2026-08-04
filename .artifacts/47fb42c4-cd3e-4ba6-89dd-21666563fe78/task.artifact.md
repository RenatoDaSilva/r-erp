# Tasks - Add Product to Order

- [x] Update `SupabaseService.kt`
    - [x] Add `SupabaseElectibleOrder` data class
    - [x] Add `getElectibleOrders` and `createOrderItem` to `SupabaseService` interface
- [x] Update `ProductsScreen.kt`
    - [x] Add "Adicionar ao pedido ..." to `ProductItem` long-press menu
    - [x] Add state for "Add to Order" dialog
    - [x] Implement `AddToOrderDialog` composable
    - [x] Handle "Adicionar" action (API call, success/error messages)
- [x] Verify the implementation
