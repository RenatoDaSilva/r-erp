# Walkthrough - Filterable Product Selection

I have implemented the filterable product selection and alphabetical sorting in the following screens:
- `AddPurchaseItemScreen`
- `AddOrderItemScreen`
- `AddBudgetItemScreen`

## Key Changes

### 1. Alphabetical Sorting
The lists of products and services are now sorted alphabetically by their description when loaded from the Supabase service.

### 2. Filterable Selector
- The product/service dropdown is now an editable `OutlinedTextField`.
- As the user types, the list is filtered in real-time to show only matching items.
- The dropdown menu automatically opens when typing and closes when an item is selected or dismissed.

### 3. State Management
- A new `searchText` state was added to handle the input in the filter field.
- In screens with a "Service" toggle, the search text and selection are cleared when switching modes to maintain consistency.
- The "Add" button's enabled state still depends on a formal selection from the dropdown list.

## Verification
- Verified that `products` and `services` are sorted using `.sortedBy { it.description?.lowercase() }`.
- Verified that `ExposedDropdownMenu` uses a filtered list based on the input text.
- Verified that the UI uses `ExposedDropdownMenuAnchorType.PrimaryEditable` for proper integration with the editable text field.
