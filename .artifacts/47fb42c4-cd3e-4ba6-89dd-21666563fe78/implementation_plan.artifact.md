# Implementation Plan - Filterable Product Selection

This plan aims to improve the product selection UX in `AddPurchaseItemScreen`, `AddOrderItemScreen`, and `AddBudgetItemScreen` by making the product dropdown filterable and sorted alphabetically.

## Proposed Changes

### General Improvements
- Sort product and service lists alphabetically by their description when fetched from the API.
- Implement filtering logic in the product/service selection dropdowns.

### [Component Name] UI Screens

#### [MODIFY] [AddPurchaseItemScreen.kt](file:///C:/Users/borba/AndroidStudioProjects/r-erp/app/src/main/java/com/r_erp/ui/screens/AddPurchaseItemScreen.kt)
- Add `searchText` state to track user input in the product field.
- Sort the `products` list alphabetically.
- Change `OutlinedTextField` to be editable and update `searchText`.
- Filter the `products` shown in `ExposedDropdownMenu` based on `searchText`.
- Update `selectedProduct` when a product is selected from the menu.

#### [MODIFY] [AddOrderItemScreen.kt](file:///C:/Users/borba/AndroidStudioProjects/r-erp/app/src/main/java/com/r_erp/ui/screens/AddOrderItemScreen.kt)
- Add `searchText` state.
- Sort `products` and `services` lists alphabetically.
- Update `OutlinedTextField` to be editable and filter both products and services (depending on the `isService` toggle).
- Ensure `searchText` is cleared or updated correctly when toggling `isService`.

#### [MODIFY] [AddBudgetItemScreen.kt](file:///C:/Users/borba/AndroidStudioProjects/r-erp/app/src/main/java/com/r_erp/ui/screens/AddBudgetItemScreen.kt)
- Add `searchText` state.
- Sort `products` and `services` lists alphabetically.
- Update `OutlinedTextField` to be editable and filter the displayed items.

## Verification Plan

### Manual Verification
- Deploy the app to a device or emulator.
- Navigate to "Add Purchase Item", "Add Order Item", and "Add Budget Item" screens.
- Verify that the product/service lists are sorted alphabetically.
- Verify that typing in the product/service field filters the list in real-time.
- Verify that selecting an item from the filtered list correctly updates the selection and related fields (like price).
- Verify that the dropdown behavior remains functional (opening/closing/dismissing).
