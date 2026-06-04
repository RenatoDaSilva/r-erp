package com.r_erp.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.r_erp.api.SupabaseClient
import com.r_erp.api.SupabaseService
import com.r_erp.api.LocalToken
import com.r_erp.api.LocalSessionManager
import com.r_erp.api.SupabaseOrderItem
import com.r_erp.api.SupabaseOrderItemRequest
import com.r_erp.api.SupabaseProduct
import com.r_erp.api.SupabaseServiceItem
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailsScreen(orderId: Int? = null, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val token = LocalToken.current
    val sessionManager = LocalSessionManager.current
    val supabaseService = remember(token) { SupabaseService.create(token, sessionManager) }

    var isLoading by remember { mutableStateOf(true) }
    var nextId by remember { mutableStateOf<Int?>(orderId) }
    
    // Order Header Data
    var selectedClient by remember { mutableStateOf<SupabaseClient?>(null) }
    var discount by remember { mutableStateOf("0.00") }
    var message by remember { mutableStateOf("") }
    var budgetIdStored by remember { mutableStateOf<Int?>(null) }
    
    // Items list (in memory)
    val orderItems = remember { mutableStateListOf<SupabaseOrderItem>() }
    
    // Options lists
    var clients by remember { mutableStateOf<List<SupabaseClient>>(emptyList()) }
    var allProducts by remember { mutableStateOf<List<SupabaseProduct>>(emptyList()) }
    var allServices by remember { mutableStateOf<List<SupabaseServiceItem>>(emptyList()) }
    var clientExpanded by remember { mutableStateOf(false) }
    
    // Sub-screen state
    var isAddingItem by remember { mutableStateOf(false) }
    var errorDialogMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(supabaseService) {
        try {
            // Load master data
            clients = supabaseService.getClients().sortedBy { it.fullName?.lowercase() ?: "" }
            allProducts = supabaseService.getProducts()
            allServices = supabaseService.getServices()

            if (orderId == null) {
                // Creation mode: Get Next Sequence ID
                nextId = supabaseService.getSequence(mapOf("sequence_name" to "orders_id_seq"))
            } else {
                // Edition mode: Fetch existing order
                val fetchedOrders = supabaseService.getOrderWithItems(idFilter = "eq.$orderId")
                if (fetchedOrders.isNotEmpty()) {
                    val o = fetchedOrders[0]
                    
                    selectedClient = clients.find { it.id == o.clientId } 
                        ?: clients.find { it.fullName == o.clientName }
                    
                    discount = String.format(Locale.US, "%.2f", o.discount ?: 0.0)
                    message = o.message ?: ""
                    budgetIdStored = o.budgetId
                    
                    // Load items directly from order_items table to get productId/serviceId
                    orderItems.clear()
                    val itemsFromTable = supabaseService.getOrderItems(orderIdFilter = "eq.$orderId")
                    if (itemsFromTable.isNotEmpty()) {
                        orderItems.addAll(itemsFromTable)
                    } else if (o.items != null) {
                        // Fallback to view items if table is empty
                        orderItems.addAll(o.items)
                    }
                } else {
                    Toast.makeText(context, "Pedido não encontrado", Toast.LENGTH_SHORT).show()
                    onBack()
                }
            }
            
            isLoading = false
        } catch (e: Exception) {
            if (e.message?.contains("composition") != true) {
                Toast.makeText(context, "Erro ao carregar dados: ${e.message}", Toast.LENGTH_LONG).show()
            }
            isLoading = false
        }
    }

    if (isAddingItem) {
        AddOrderItemScreen(
            onItemAdded = { newItem ->
                orderItems.add(newItem)
                isAddingItem = false
            },
            onCancel = { isAddingItem = false }
        )
        return
    }

    Scaffold { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val title = if (orderId == null) "Novo Pedido" else "Editar Pedido"
                Text(text = "$title ID: ${nextId ?: "..."}", style = MaterialTheme.typography.headlineSmall)

                // Client Selection
                ExposedDropdownMenuBox(
                    expanded = clientExpanded,
                    onExpandedChange = { clientExpanded = !clientExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedClient?.fullName ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Cliente") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = clientExpanded) },
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = clientExpanded,
                        onDismissRequest = { clientExpanded = false }
                    ) {
                        clients.forEach { client ->
                            DropdownMenuItem(
                                text = { Text(client.fullName ?: "") },
                                onClick = {
                                    selectedClient = client
                                    clientExpanded = false
                                }
                            )
                        }
                    }
                }

                // Discount
                OutlinedTextField(
                    value = discount,
                    onValueChange = { discount = it },
                    label = { Text("Desconto Total") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Message
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Observações") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Itens do Pedido", style = MaterialTheme.typography.titleMedium)
                    Button(onClick = { isAddingItem = true }) {
                        Text("Novo ítem ...")
                    }
                }

                // Summary of items
                orderItems.forEachIndexed { index, item ->
                    val displayDescription = item.description 
                        ?: allProducts.find { it.id == item.productId }?.description 
                        ?: allServices.find { it.id == item.serviceId }?.description 
                        ?: "Item desconhecido"
                    
                    val itemTotal = if ((item.total ?: 0.0) == 0.0) {
                        ((item.quantity ?: 0.0) * (item.price ?: 0.0)) - (item.discount ?: 0.0)
                    } else {
                        item.total!!
                    }

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(text = displayDescription, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    Text(text = "x${String.format(Locale.US, "%.2f", item.quantity ?: 0.0)}")
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(text = "Preço: ${String.format(Locale.US, "%.2f", item.price ?: 0.0)}", style = MaterialTheme.typography.bodySmall)
                                    Text(text = "Total: ${String.format(Locale.US, "%.2f", itemTotal)}", fontWeight = FontWeight.Bold)
                                }
                            }
                            IconButton(onClick = { orderItems.removeAt(index) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remover item", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        if (index < orderItems.size - 1) HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { 
                            if (selectedClient == null) {
                                Toast.makeText(context, "Selecione um cliente", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (orderItems.isEmpty()) {
                                Toast.makeText(context, "Adicione pelo menos um item", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            scope.launch {
                                try {
                                    val orderMap = mutableMapOf<String, Any>()
                                    orderMap["id"] = nextId!!
                                    orderMap["client_id"] = selectedClient!!.id!!
                                    orderMap["discount"] = discount.toDoubleOrNull() ?: 0.0
                                    if (message.isNotBlank()) orderMap["message"] = message
                                    if (budgetIdStored != null) orderMap["budget_id"] = budgetIdStored!!

                                    if (orderId == null) {
                                        // 1. Save Order Header (New)
                                        val response = supabaseService.createOrder(orderMap)
                                        if (!response.isSuccessful) throw retrofit2.HttpException(response)
                                    } else {
                                        // 1. Update Order Header (Existing)
                                        val response = supabaseService.updateOrder(idFilter = "eq.$orderId", order = orderMap)
                                        if (!response.isSuccessful) throw retrofit2.HttpException(response)
                                        
                                        // 2. Clear old items
                                        val delResponse = supabaseService.deleteOrderItems(orderIdFilter = "eq.$orderId")
                                        if (!delResponse.isSuccessful) throw retrofit2.HttpException(delResponse)
                                    }

                                    // 3. Prepare and Save Items
                                    val itemsRequest = orderItems.map { 
                                        SupabaseOrderItemRequest(
                                            orderId = nextId,
                                            productId = it.productId,
                                            serviceId = it.serviceId,
                                            quantity = it.quantity,
                                            price = it.price,
                                            discount = it.discount
                                        )
                                    }
                                    if (itemsRequest.isNotEmpty()) {
                                        val itemsResponse = supabaseService.createOrderItems(itemsRequest)
                                        if (!itemsResponse.isSuccessful) throw retrofit2.HttpException(itemsResponse)
                                    }

                                    Toast.makeText(context, "Pedido salvo com sucesso!", Toast.LENGTH_SHORT).show()
                                    onBack()
                                } catch (e: Exception) {
                                    val errorBody = (e as? retrofit2.HttpException)?.response()?.errorBody()?.string()
                                    Log.e("OrderDetails", "Error saving order: $errorBody", e)
                                    val errorMessage = if (e is retrofit2.HttpException) {
                                        "Erro ${e.code()}: ${errorBody ?: e.message()}"
                                    } else {
                                        e.message ?: "Erro desconhecido"
                                    }
                                    errorDialogMessage = errorMessage
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Salvar")
                    }
                    TextButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                        Text("Cancelar")
                    }
                }
            }
        }
    }

    if (errorDialogMessage != null) {
        AlertDialog(
            onDismissRequest = { errorDialogMessage = null },
            title = { Text("Erro ao Salvar") },
            text = { Text(errorDialogMessage!!) },
            confirmButton = {
                TextButton(onClick = { errorDialogMessage = null }) {
                    Text("OK")
                }
            }
        )
    }
}
