package com.r_erp.ui.screens

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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

    suspend fun refreshProducts(scrollToProductId: Int? = null) {
        try {
            isLoading = true
            products = supabaseService.getProducts()
            errorMessage = null
            
            if (scrollToProductId != null) {
                val index = filteredProducts.indexOfFirst { it.id == scrollToProductId }
                if (index >= 0) {
                    listState.animateScrollToItem(index)
                }
            }
        } catch (e: Exception) {
            errorMessage = e.message ?: e.toString()
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(supabaseService) {
        refreshProducts()
    }

    var showAdjustDialog by remember { mutableStateOf(false) }
    var adjustmentType by remember { mutableStateOf("") }
    var adjustmentProduct by remember { mutableStateOf<SupabaseProduct?>(null) }
    var adjustmentValue by remember { mutableStateOf("1") }

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
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center),
                    )
                } else if (products.isEmpty()) {
                    Text(
                        text = "Nenhum produto encontrado.",
                        modifier = Modifier.align(Alignment.Center),
                    )
                } else if (filteredProducts.isEmpty()) {
                    Text(
                        text = "Nenhum produto corresponde ao filtro.",
                        modifier = Modifier.align(Alignment.Center),
                    )
                } else {
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
                                }
                            )
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
    onAdjustStock: (String) -> Unit
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
