# Walkthrough - Add Service to Order

I have implemented the "Adicionar ao pedido ..." feature in the Services screen.

## Changes Made

### UI Enhancements
- **[ServicesScreen.kt](file:///C:/Users/borba/AndroidStudioProjects/r-erp/app/src/main/java/com/r_erp/ui/screens/ServicesScreen.kt)**:
    - Added a long-press popup menu to each service item.
    - Added "Adicionar ao pedido ..." to the menu.
    - Implemented `AddServiceToOrderDialog`:
        - **Electible Order Selection**: Reuses the filterable combo box to search and select an existing order.
        - **Quantity Input**: A numeric field to specify the quantity to add.
        - **Price Input**: A numeric field pre-filled with the service's current price, allowing for adjustments.
        - **Feedback**: Displays a success Toast message upon completion or an error message if the operation fails.

## Verification
- Verified that the long-press triggers the "Adicionar ao pedido ..." menu item.
- Verified that the dialog opens with the correct service details and pre-filled price.
- Verified the order search and selection functionality.
- Verified that the "Adicionar" button correctly POSTs the data and handles success/error cases.
