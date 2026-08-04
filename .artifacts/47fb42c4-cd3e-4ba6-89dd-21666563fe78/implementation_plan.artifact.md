# Implementation Plan - Add Service to Order

This plan adds a feature to the Services screen allowing users to long-press a service and add it to an existing "electible" order, with the ability to adjust the price and quantity.

## Proposed Changes

### [Component Name] UI Layer

#### [MODIFY] [ServicesScreen.kt](file:///C:/Users/borba/AndroidStudioProjects/r-erp/app/src/main/java/com/r_erp/ui/screens/ServicesScreen.kt)
- Update `ServicesScreen` to include state for "Add to Order" dialog:
    - `showAddToOrderDialog: Boolean`
    - `serviceToAddToOrder: SupabaseServiceItem?`
- Update `ServiceItem` to:
    - Add `onAddToOrder: () -> Unit` callback.
    - Display a `DropdownMenu` item "Adicionar ao pedido ..." on long click.
- Implement `AddServiceToOrderDialog` composable:
    - Fetches electible orders using `supabaseService.getElectibleOrders()` on launch.
    - Displays a filterable `ExposedDropdownMenuBox` to select an order.
    - Includes an `OutlinedTextField` for quantity (numeric keyboard, defaults to 1.00).
    - Includes an `OutlinedTextField` for price (numeric keyboard, pre-filled with the service's current price).
    - Includes "Adicionar" and "Cancelar" buttons.
    - On "Adicionar":
        - POSTs to `order_items` via `supabaseService.createOrderItem()` with `order_id`, `service_id`, `price`, and `quantity`.
        - Shows a success message (Toast) "Serviço adicionado ao pedido [id] com sucesso" and closes the dialog on success.
        - Shows an error message and keeps the dialog open on failure.

## Verification Plan

### Manual Verification
1.  Open the app and navigate to the Services screen.
2.  Long-press a service item.
3.  Verify "Adicionar ao pedido ..." appears in the menu.
4.  Click "Adicionar ao pedido ...".
5.  Verify the dialog opens and shows the service description.
6.  Verify the combo box lists electible orders.
7.  Verify the price field is pre-filled with the service's price.
8.  Modify quantity and/or price.
9.  Select an order and click "Adicionar".
10. Verify the success message "Serviço adicionado ao pedido [id] com sucesso" appears and the dialog closes.
11. Test the "Cancelar" button to ensure it closes the dialog without action.
