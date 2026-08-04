package com.r_erp.ui.screens

import android.widget.Toast
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.r_erp.api.SupabaseService
import com.r_erp.api.SupabaseServiceItem
import com.r_erp.api.LocalToken
import com.r_erp.api.LocalSessionManager
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun ServicesScreen(onServiceClick: (Int) -> Unit) {
    val token = LocalToken.current
    val sessionManager = LocalSessionManager.current
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    var services by remember { mutableStateOf<List<SupabaseServiceItem>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(value = true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var showAddToBudgetDialog by remember { mutableStateOf(false) }
    var serviceToAddToBudget by remember { mutableStateOf<SupabaseServiceItem?>(null) }

    var showAddToOrderDialog by remember { mutableStateOf(false) }
    var serviceToAddToOrder by remember { mutableStateOf<SupabaseServiceItem?>(null) }

    val supabaseService = remember(token) { SupabaseService.create(token, sessionManager) }

    if (showAddToBudgetDialog && serviceToAddToBudget != null) {
        AddServiceToBudgetDialog(
            service = serviceToAddToBudget!!,
            supabaseService = supabaseService,
            onDismiss = { showAddToBudgetDialog = false },
            onSuccess = { budgetId ->
                Toast.makeText(context, "Serviço adicionado ao orçamento $budgetId com sucesso", Toast.LENGTH_LONG).show()
                showAddToBudgetDialog = false
            }
        )
    }

    if (showAddToOrderDialog && serviceToAddToOrder != null) {
        AddServiceToOrderDialog(
            service = serviceToAddToOrder!!,
            supabaseService = supabaseService,
            onDismiss = { showAddToOrderDialog = false },
            onSuccess = { orderId ->
                Toast.makeText(context, "Serviço adicionado ao pedido $orderId com sucesso", Toast.LENGTH_LONG).show()
                showAddToOrderDialog = false
            }
        )
    }

    val filteredServices = remember(searchQuery, services) {
        if (searchQuery.isBlank()) {
            services
        } else {
            services.filter {
                it.description?.contains(searchQuery, ignoreCase = true) == true
            }
        }
    }

    LaunchedEffect(supabaseService) {
        try {
            if (services.isEmpty()) isLoading = true
            errorMessage = null
            services = supabaseService.getServices()
            isLoading = false
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            val msg = e.message ?: e.toString()
            if (!msg.contains("composition", ignoreCase = true)) {
                errorMessage = if (msg.contains("401")) {
                    "Sessão expirada. Por favor, saia e entre novamente."
                } else {
                    msg
                }
            }
            isLoading = false
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { onServiceClick(-1) }) {
                Icon(Icons.Default.Add, contentDescription = "Novo Serviço")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Keep the search field always in the composition for focus stability
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Filtrar por descrição...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpar")
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        focusManager.clearFocus()
                    }
                ),
                enabled = !isLoading || services.isNotEmpty()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                if (isLoading && services.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (errorMessage != null && services.isEmpty()) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center),
                    )
                } else if (services.isEmpty() && !isLoading) {
                    Text(
                        text = "Nenhum serviço encontrado.",
                        modifier = Modifier.align(Alignment.Center),
                    )
                } else if (filteredServices.isEmpty() && !isLoading) {
                    Text(
                        text = "Nenhum serviço corresponde ao filtro.",
                        modifier = Modifier.align(Alignment.Center),
                    )
                } else {
                    Column {
                        if (isLoading) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                        ) {
                            items(
                                items = filteredServices,
                                key = { service: com.r_erp.api.SupabaseServiceItem -> service.id ?: 0 }
                            ) { service: com.r_erp.api.SupabaseServiceItem ->
                                ServiceItem(
                                    service = service,
                                    onClick = { 
                                        focusManager.clearFocus()
                                        onServiceClick(service.id ?: 0) 
                                    },
                                    onAddToBudget = {
                                        serviceToAddToBudget = service
                                        showAddToBudgetDialog = true
                                    },
                                    onAddToOrder = {
                                        serviceToAddToOrder = service
                                        showAddToOrderDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ServiceItem(service: SupabaseServiceItem, onClick: () -> Unit, onAddToBudget: () -> Unit, onAddToOrder: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }

    Box {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showMenu = true }
                ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
            ) {
                Text(text = "ID: ${service.id ?: "N/A"}", style = MaterialTheme.typography.labelMedium)
                Text(text = service.description ?: "Sem descrição", style = MaterialTheme.typography.titleLarge)
                
                Text(
                    text = "Preço: ${String.format(Locale.US, "%.2f", service.price ?: 0.0)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Adicionar ao orçamento ...") },
                onClick = {
                    showMenu = false
                    onAddToBudget()
                }
            )
            DropdownMenuItem(
                text = { Text("Adicionar ao pedido ...") },
                onClick = {
                    showMenu = false
                    onAddToOrder()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddServiceToBudgetDialog(
    service: SupabaseServiceItem,
    supabaseService: SupabaseService,
    onDismiss: () -> Unit,
    onSuccess: (Int) -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var budgets by remember { mutableStateOf<List<com.r_erp.api.SupabaseElectibleBudget>>(emptyList()) }
    var selectedBudget by remember { mutableStateOf<com.r_erp.api.SupabaseElectibleBudget?>(null) }
    var quantity by remember { mutableStateOf("1.00") }
    var price by remember { mutableStateOf(String.format(Locale.US, "%.2f", service.price ?: 0.0)) }
    var searchText by remember { mutableStateOf("") }
    var isExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            budgets = supabaseService.getElectibleBudgets().sortedBy { it.clientName?.lowercase() }
            isLoading = false
        } catch (e: Exception) {
            errorMessage = e.message ?: "Erro ao carregar orçamentos"
            isLoading = false
        }
    }

    val filteredBudgets = remember(budgets, searchText) {
        budgets.filter { 
            (it.clientName ?: "").contains(searchText, ignoreCase = true) || 
            (it.id?.toString() ?: "").contains(searchText)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adicionar Serviço ao Orçamento") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "Serviço: ${service.description}", style = MaterialTheme.typography.bodyLarge)
                
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                } else {
                    ExposedDropdownMenuBox(
                        expanded = isExpanded,
                        onExpandedChange = { isExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = searchText,
                            onValueChange = { 
                                searchText = it
                                isExpanded = true
                                if (selectedBudget?.clientName != it) selectedBudget = null
                            },
                            label = { Text("Selecionar Orçamento") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true).fillMaxWidth(),
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )
                        
                        ExposedDropdownMenu(
                            expanded = isExpanded && filteredBudgets.isNotEmpty(),
                            onDismissRequest = { isExpanded = false }
                        ) {
                            filteredBudgets.forEach { budget ->
                                DropdownMenuItem(
                                    text = { Text("#${budget.id} - ${budget.clientName}") },
                                    onClick = {
                                        selectedBudget = budget
                                        searchText = budget.clientName ?: ""
                                        isExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        label = { Text("Quantidade") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("Preço") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (errorMessage != null) {
                        Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = quantity.toDoubleOrNull() ?: 0.0
                    val p = price.toDoubleOrNull() ?: 0.0
                    if (selectedBudget != null && qty > 0) {
                        isSaving = true
                        scope.launch {
                            try {
                                val request = com.r_erp.api.SupabaseBudgetItemRequest(
                                    budgetId = selectedBudget!!.id,
                                    productId = null,
                                    serviceId = service.id,
                                    quantity = qty,
                                    price = p,
                                    discount = 0.0
                                )
                                val response = supabaseService.createBudgetItem(request)
                                if (response.isSuccessful) {
                                    onSuccess(selectedBudget!!.id!!)
                                } else {
                                    errorMessage = "Erro: ${response.code()} ${response.message()}"
                                }
                            } catch (e: Exception) {
                                errorMessage = e.message ?: "Erro ao salvar"
                            } finally {
                                isSaving = false
                            }
                        }
                    }
                },
                enabled = selectedBudget != null && !isSaving && !isLoading
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Adicionar")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("Cancelar")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddServiceToOrderDialog(
    service: SupabaseServiceItem,
    supabaseService: SupabaseService,
    onDismiss: () -> Unit,
    onSuccess: (Int) -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var orders by remember { mutableStateOf<List<com.r_erp.api.SupabaseElectibleOrder>>(emptyList()) }
    var selectedOrder by remember { mutableStateOf<com.r_erp.api.SupabaseElectibleOrder?>(null) }
    var quantity by remember { mutableStateOf("1.00") }
    var price by remember { mutableStateOf(String.format(Locale.US, "%.2f", service.price ?: 0.0)) }
    var searchText by remember { mutableStateOf("") }
    var isExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            orders = supabaseService.getElectibleOrders().sortedBy { it.clientName?.lowercase() }
            isLoading = false
        } catch (e: Exception) {
            errorMessage = e.message ?: "Erro ao carregar pedidos"
            isLoading = false
        }
    }

    val filteredOrders = remember(orders, searchText) {
        orders.filter { 
            (it.clientName ?: "").contains(searchText, ignoreCase = true) || 
            (it.id?.toString() ?: "").contains(searchText)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adicionar Serviço ao Pedido") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "Serviço: ${service.description}", style = MaterialTheme.typography.bodyLarge)
                
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                } else {
                    ExposedDropdownMenuBox(
                        expanded = isExpanded,
                        onExpandedChange = { isExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = searchText,
                            onValueChange = { 
                                searchText = it
                                isExpanded = true
                                if (selectedOrder?.clientName != it) selectedOrder = null
                            },
                            label = { Text("Selecionar Pedido") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true).fillMaxWidth(),
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )
                        
                        ExposedDropdownMenu(
                            expanded = isExpanded && filteredOrders.isNotEmpty(),
                            onDismissRequest = { isExpanded = false }
                        ) {
                            filteredOrders.forEach { order ->
                                DropdownMenuItem(
                                    text = { Text("#${order.id} - ${order.clientName}") },
                                    onClick = {
                                        selectedOrder = order
                                        searchText = order.clientName ?: ""
                                        isExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        label = { Text("Quantidade") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("Preço") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (errorMessage != null) {
                        Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = quantity.toDoubleOrNull() ?: 0.0
                    val p = price.toDoubleOrNull() ?: 0.0
                    if (selectedOrder != null && qty > 0) {
                        isSaving = true
                        scope.launch {
                            try {
                                val request = com.r_erp.api.SupabaseOrderItemRequest(
                                    orderId = selectedOrder!!.id,
                                    productId = null,
                                    serviceId = service.id,
                                    quantity = qty,
                                    price = p,
                                    discount = 0.0
                                )
                                val response = supabaseService.createOrderItem(request)
                                if (response.isSuccessful) {
                                    onSuccess(selectedOrder!!.id!!)
                                } else {
                                    errorMessage = "Erro: ${response.code()} ${response.message()}"
                                }
                            } catch (e: Exception) {
                                errorMessage = e.message ?: "Erro ao salvar"
                            } finally {
                                isSaving = false
                            }
                        }
                    }
                },
                enabled = selectedOrder != null && !isSaving && !isLoading
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Adicionar")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("Cancelar")
            }
        }
    )
}
