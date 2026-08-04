# Walkthrough - Add Product to Budget

I have implemented the "Adicionar ao orçamento ..." feature in the Products screen.

## Changes Made

### 1. API Integration
- **[SupabaseService.kt](file:///C:/Users/borba/AndroidStudioProjects/r-erp/app/src/main/java/com/r_erp/api/SupabaseService.kt)**:
    - Added `SupabaseElectibleBudget` data class to represent budgets eligible for adding products.
    - Added `getElectibleBudgets()` to fetch the list from `https://euzmbicrbjpgcyrojvdm.supabase.co/rest/v1/electible_budgets`.
    - Added `createBudgetItem()` to POST a single item to `https://euzmbicrbjpgcyrojvdm.supabase.co/rest/v1/budget_items`.

### 2. UI Enhancements
- **[ProductsScreen.kt](file:///C:/Users/borba/AndroidStudioProjects/r-erp/app/src/main/java/com/r_erp/ui/screens/ProductsScreen.kt)**:
    - Added "Adicionar ao orçamento ..." to the long-press menu of each product item.
    - Implemented `AddToBudgetDialog`:
        - A popup dialog that appears when "Adicionar ao orçamento ..." is selected.
        - Includes a filterable combo box to search and select an existing budget.
        - Includes a numeric input field for the quantity.
        - "Adicionar" button triggers the API call with the correct payload (`budget_id`, `product_id`, `price`, `quantity`).
        - Displays a success Toast message upon successful addition and closes the dialog.
        - Displays an error message within the dialog if the operation fails, keeping the dialog open for correction.
        - "Cancelar" button dismisses the dialog without any action.

## Verification
- Verified the data models match the Supabase table structures.
- Verified the long-press interaction triggers the dialog.
- Verified the search/filter functionality in the budget selection combo box.
- Verified the error handling and success feedback loop.
