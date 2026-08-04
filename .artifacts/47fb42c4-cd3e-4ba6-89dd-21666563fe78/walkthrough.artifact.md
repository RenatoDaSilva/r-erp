# Walkthrough - Add Product to Order

I have implemented the "Adicionar ao pedido ..." feature in the Products screen.

## Changes Made

### 1. API Integration
- **[SupabaseService.kt](file:///C:/Users/borba/AndroidStudioProjects/r-erp/app/src/main/java/com/r_erp/api/SupabaseService.kt)**:
    - Added `SupabaseElectibleOrder` data class to represent orders eligible for adding products.
    - Added `getElectibleOrders()` to fetch the list from `https://euzmbicrbjpgcyrojvdm.supabase.co/rest/v1/electible_orders`.
    - Added `createOrderItem()` to POST a single item to `https://euzmbicrbjpgcyrojvdm.supabase.co/rest/v1/order_items`.

### 2. UI Enhancements
- **[ProductsScreen.kt](file:///C:/Users/borba/AndroidStudioProjects/r-erp/app/src/main/java/com/r_erp/ui/screens/ProductsScreen.kt)**:
    - Added "Adicionar ao pedido ..." to the long-press menu of each product item.
    - Implemented `AddToOrderDialog`:
        - A popup dialog that appears when "Adicionar ao pedido ..." is selected.
        - Includes a filterable search box to select an existing order.
        - Includes a numeric input field for the quantity.
        - "Adicionar" button triggers the API call with the correct payload (`order_id`, `product_id`, `price`, `quantity`).
        - Displays a success Toast message upon successful addition and closes the dialog.
        - Displays an error message within the dialog if the operation fails.
        - "Cancelar" button dismisses the dialog without any action.

## Verification
- Verified the data models match the requested Supabase endpoints.
- Verified the long-press menu triggers the new dialog.
- Verified the filterable search functionality for orders.
- Verified success and error handling in the dialog.
