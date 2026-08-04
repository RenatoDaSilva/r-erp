package com.r_erp.ui.screens

import android.widget.Toast
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.r_erp.api.SupabaseProduct
import com.r_erp.api.LocalToken
import com.r_erp.api.LocalSessionManager
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun ProductsScreen(onProductClick: (Int) -> Unit) {
    val token = LocalToken.current
    val sessionManager = LocalSessionManager.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    
    var products by remember { mutableStateOf<List<SupabaseProduct>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(value = true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val listState = rememberLazyListState()

    val supabaseService = remember(token) { SupabaseService.create(token, sessionManager) }

    val filteredProducts = remember(searchQuery, products) {
        if (searchQuery.isBlank()) {
            products
        } else {
            products.filter {
                it.description?.contains(searchQuery, ignoreCase = true) == true
            }
        }
    }

    suspend fun refreshProducts(scrollToProductId: Int? = null, showSpinner: Boolean = true) {
        try {
            if (showSpinner) isLoading = true
            products = supabaseService.getProducts()
            errorMessage = null
            
            if (scrollToProductId != null) {
                val index = filteredProducts.indexOfFirst { it.id == scrollToProductId }
                if (index >= 0) {
                    listState.animateScrollToItem(index)
                }
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            val msg = e.message ?: e.toString()
            if (!msg.contains("composition", ignoreCase = true)) {
                errorMessage = msg
            }
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(supabaseService) {
        refreshProducts(showSpinner = products.isEmpty())
    }

    var showAdjustDialog by remember { mutableStateOf(false) }
    var adjustmentType by remember { mutableStateOf("") }
    var adjustmentProduct by remember { mutableStateOf<SupabaseProduct?>(null) }
    var adjustmentValue by remember { mutableStateOf("1") }

    var showAddToBudgetDialog by remember { mutableStateOf(false) }
    var productToAddToBudget by remember { mutableStateOf<SupabaseProduct?>(null) }

    var showAddToOrderDialog by remember { mutableStateOf(false) }
    var productToAddToOrder by remember { mutableStateOf<SupabaseProduct?>(null) }

    val context = LocalContext.current

    if (showAddToBudgetDialog && productToAddToBudget != null) {
        AddToBudgetDialog(
            product = productToAddToBudget!!,
            supabaseService = supabaseService,
            onDismiss = { showAddToBudgetDialog = false },
            onSuccess = { budgetId ->
                Toast.makeText(context, "Produto adicionado ao orçamento $budgetId com sucesso", Toast.LENGTH_LONG).show()
                showAddToBudgetDialog = false
            }
        )
    }

    if (showAddToOrderDialog && productToAddToOrder != null) {
        AddToOrderDialog(
            product = productToAddToOrder!!,
            supabaseService = supabaseService,
            onDismiss = { showAddToOrderDialog = false },
            onSuccess = { orderId ->
                Toast.makeText(context, "Produto adicionado ao pedido $orderId com sucesso", Toast.LENGTH_LONG).show()
                showAddToOrderDialog = false
            }
        )
    }

    if (showAdjustDialog && adjustmentProduct != null) {
        AlertDialog(
            onDismissRequest = { showAdjustDialog = false },
            title = { Text(when(adjustmentType) {
                "ajuste" -> "Ajustar estoque"
                "perda" -> "Informar perda"
                "recarga" -> "Informar recarga"
                else -> ""
            }) },
            text = {
                Column {
                    Text(adjustmentProduct?.description ?: "")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = adjustmentValue,
                        onValueChange = { adjustmentValue = it },
                        label = { Text("Quantidade") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val qty = adjustmentValue.toDoubleOrNull() ?: 0.0
                    scope.launch {
                        try {
                            supabaseService.adjustStock(mapOf(
                                "p_id" to adjustmentProduct!!.id!!,
                                "p_quantity" to qty,
                                "p_type" to adjustmentType
                            ))
                            showAdjustDialog = false
                            refreshProducts(scrollToProductId = adjustmentProduct!!.id)
                        } catch (e: Exception) {
                            errorMessage = "Erro ao ajustar estoque: ${e.message}"
                        }
                    }
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdjustDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { onProductClick(-1) }) {
                Icon(Icons.Default.Add, contentDescription = "Novo Produto")
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
                        IconButton(onClick = { 
                            searchQuery = ""
                        }) {
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
                enabled = !isLoading || products.isNotEmpty()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                if (isLoading && products.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (errorMessage != null && products.isEmpty()) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center),
                    )
                } else if (products.isEmpty() && !isLoading) {
                    Text(
                        text = "Nenhum produto encontrado.",
                        modifier = Modifier.align(Alignment.Center),
                    )
                } else if (filteredProducts.isEmpty() && !isLoading) {
                    Text(
                        text = "Nenhum produto corresponde ao filtro.",
                        modifier = Modifier.align(Alignment.Center),
                    )
                } else {
                    Column {
                        if (isLoading) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                        ) {
                            items(
                                items = filteredProducts,
                                key = { product: SupabaseProduct -> product.id ?: 0 }
                            ) { product: SupabaseProduct ->
                                ProductItem(
                                    product = product,
                                    onClick = { 
                                        focusManager.clearFocus()
                                        onProductClick(product.id ?: 0) 
                                    },
                                    onAdjustStock = { type ->
                                        adjustmentProduct = product
                                        adjustmentType = type
                                        adjustmentValue = "1"
                                        showAdjustDialog = true
                                    },
                                    onAddToBudget = {
                                        productToAddToBudget = product
                                        showAddToBudgetDialog = true
                                    },
                                    onAddToOrder = {
                                        productToAddToOrder = product
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
fun ProductItem(
    product: SupabaseProduct,
    onClick: () -> Unit,
    onAdjustStock: (String) -> Unit,
    onAddToBudget: () -> Unit,
    onAddToOrder: () -> Unit
) {
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
                Text(text = "ID: ${product.id ?: "N/A"}", style = MaterialTheme.typography.labelMedium)
                Text(text = product.description ?: "Sem descrição", style = MaterialTheme.typography.titleLarge)
                
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Tipo: ${product.type ?: "N/A"}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Un.: ${product.unit ?: "N/A"}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Preço: ${String.format(Locale.US, "%.2f", product.price ?: 0.0)}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Estoque: ${String.format(Locale.US, "%.2f", product.stock ?: 0.0)}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                }

                product.cost?.let {
                    Text(
                        text = "Custo: ${String.format(Locale.US, "%.2f", it)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
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

            val stockEnabled = product.generatesStock == true
            
            DropdownMenuItem(
                text = { Text("Ajustar estoque...") },
                onClick = {
                    showMenu = false
                    onAdjustStock("ajuste")
                },
                enabled = stockEnabled
            )
            DropdownMenuItem(
                text = { Text("Perda...") },
                onClick = {
                    showMenu = false
                    onAdjustStock("perda")
                },
                enabled = stockEnabled
            )
            DropdownMenuItem(
                text = { Text("Recarga...") },
                onClick = {
                    showMenu = false
                    onAdjustStock("recarga")
                },
                enabled = stockEnabled
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToBudgetDialog(
    product: SupabaseProduct,
    supabaseService: SupabaseService,
    onDismiss: () -> Unit,
    onSuccess: (Int) -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var budgets by remember { mutableStateOf<List<com.r_erp.api.SupabaseElectibleBudget>>(emptyList()) }
    var selectedBudget by remember { mutableStateOf<com.r_erp.api.SupabaseElectibleBudget?>(null) }
    var quantity by remember { mutableStateOf("1.00") }
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
        title = { Text("Adicionar ao orçamento") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "Produto: ${product.description}", style = MaterialTheme.typography.bodyLarge)
                
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
                    if (selectedBudget != null && qty > 0) {
                        isSaving = true
                        scope.launch {
                            try {
                                val request = com.r_erp.api.SupabaseBudgetItemRequest(
                                    budgetId = selectedBudget!!.id,
                                    productId = product.id,
                                    serviceId = null,
                                    quantity = qty,
                                    price = product.price,
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
fun AddToOrderDialog(
    product: SupabaseProduct,
    supabaseService: SupabaseService,
    onDismiss: () -> Unit,
    onSuccess: (Int) -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var orders by remember { mutableStateOf<List<com.r_erp.api.SupabaseElectibleOrder>>(emptyList()) }
    var selectedOrder by remember { mutableStateOf<com.r_erp.api.SupabaseElectibleOrder?>(null) }
    var quantity by remember { mutableStateOf("1.00") }
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
        title = { Text("Adicionar ao pedido") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "Produto: ${product.description}", style = MaterialTheme.typography.bodyLarge)
                
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
                    if (selectedOrder != null && qty > 0) {
                        isSaving = true
                        scope.launch {
                            try {
                                val request = com.r_erp.api.SupabaseOrderItemRequest(
                                    orderId = selectedOrder!!.id,
                                    productId = product.id,
                                    serviceId = null,
                                    quantity = qty,
                                    price = product.price,
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
