package com.r_erp.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.r_erp.api.LocalToken
import com.r_erp.api.LocalSessionManager
import com.r_erp.api.SupabaseSupplier
import com.r_erp.api.SupabasePurchase
import com.r_erp.api.SupabaseService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@Composable
fun PurchasesScreen(onAddPurchase: () -> Unit, onPurchaseClick: (Int) -> Unit) {
    val context = LocalContext.current
    val token = LocalToken.current
    val sessionManager = LocalSessionManager.current
    val supabaseService = remember(token) { SupabaseService.create(token, sessionManager) }
    var purchases by remember { mutableStateOf<List<SupabasePurchase>>(emptyList()) }
    var suppliers by remember { mutableStateOf<List<SupabaseSupplier>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(value = true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isScanningNfce by remember { mutableStateOf(false) }
    var isPostingNfce by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val supplierMap = remember(suppliers) { suppliers.associate { it.id to it.fullName } }

    val filteredPurchases = remember(searchQuery, purchases, supplierMap) {
        if (searchQuery.isBlank()) {
            purchases
        } else {
            purchases.filter {
                val supplierName = it.supplierName ?: supplierMap[it.supplierId] ?: ""
                supplierName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    fun loadData() {
        isLoading = true
        scope.launch {
            try {
                suppliers = supabaseService.getSuppliers()
                purchases = supabaseService.getPurchasesWithItems()
                isLoading = false
            } catch (e: Exception) {
                if (e.message?.contains("composition") != true) {
                    errorMessage = e.message ?: e.toString()
                }
                isLoading = false
            }
        }
    }

    LaunchedEffect(supabaseService) {
        loadData()
    }

    if (isScanningNfce) {
        NfceScannerScreen(
            onNfceDataExtracted = { data ->
                isScanningNfce = false
                isPostingNfce = true
                scope.launch {
                    try {
                        val response = supabaseService.buildPurchase(data)
                        if (response.isSuccessful) {
                            val result = response.body()
                            if (result != null && result.isJsonPrimitive && result.asJsonPrimitive.isNumber) {
                                val newId = result.asJsonPrimitive.asInt
                                Toast.makeText(context, "Compra $newId registrada!", Toast.LENGTH_SHORT).show()
                                
                                // Refresh and scroll
                                suppliers = supabaseService.getSuppliers()
                                purchases = supabaseService.getPurchasesWithItems()
                                
                                delay(1000)
                                val index = purchases.indexOfFirst { it.id == newId }
                                if (index != -1) {
                                    listState.animateScrollToItem(index)
                                }
                            } else {
                                Toast.makeText(context, result?.asString ?: "Erro inesperado", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(context, "Erro: ${response.message()}", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Erro ao processar: ${e.message}", Toast.LENGTH_LONG).show()
                    } finally {
                        isPostingNfce = false
                    }
                }
            },
            onCancel = { isScanningNfce = false }
        )
        return
    }

    Scaffold(
        floatingActionButton = {
            var showFabMenu by remember { mutableStateOf(false) }
            Box {
                FloatingActionButton(onClick = { showFabMenu = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Menu Compras")
                }
                DropdownMenu(expanded = showFabMenu, onDismissRequest = { showFabMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Novo") },
                        onClick = {
                            showFabMenu = false
                            onAddPurchase()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Ler NFS-e...") },
                        onClick = {
                            showFabMenu = false
                            isScanningNfce = true
                        }
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isPostingNfce) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (!isLoading && errorMessage == null && purchases.isNotEmpty()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Filtrar por fornecedor...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpar")
                            }
                        }
                    },
                    singleLine = true
                )
            }

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
                } else if (purchases.isEmpty()) {
                    Text(
                        text = "Nenhuma compra encontrada.",
                        modifier = Modifier.align(Alignment.Center),
                    )
                } else if (filteredPurchases.isEmpty()) {
                    Text(
                        text = "Nenhum compra corresponde ao filtro.",
                        modifier = Modifier.align(Alignment.Center),
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        items(filteredPurchases) { purchase ->
                            PurchaseItem(
                                purchase, 
                                supplierName = purchase.supplierName ?: supplierMap[purchase.supplierId] ?: "N/A",
                                onClick = { onPurchaseClick(purchase.id ?: 0) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PurchaseItem(purchase: SupabasePurchase, supplierName: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .combinedClickable(
                onClick = onClick
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "Data: ${formatDate(purchase.createdAt)}",
                    style = MaterialTheme.typography.labelMedium
                )
            }
            
            Text(
                text = "Fornecedor: $supplierName",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Pagar até: ${formatDate(purchase.payUntil)}",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Items table
            purchase.items?.let { items ->
                val displayItems = items.take(3)
                
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(text = "Desc.", modifier = Modifier.weight(3f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text(text = "Qtd", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text(text = "Preço", modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text(text = "Desc.", modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text(text = "Total", modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                    
                    displayItems.forEach { item ->
                        val itemDisplayName = item.description ?: item.product?.description ?: "Produto ${item.productId}"
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(text = itemDisplayName, modifier = Modifier.weight(3f), fontSize = 10.sp, maxLines = 1)
                            Text(text = formatDecimal(item.quantity), modifier = Modifier.weight(1f), fontSize = 10.sp)
                            Text(text = formatDecimal(item.price), modifier = Modifier.weight(1.5f), fontSize = 10.sp)
                            Text(text = formatDecimal(item.discount), modifier = Modifier.weight(1.5f), fontSize = 10.sp)
                            Text(text = formatDecimal(item.total), modifier = Modifier.weight(1.5f), fontSize = 10.sp)
                        }
                    }

                    if (items.size > 3) {
                        val remaining = (purchase.itemsCount ?: items.size) - 3
                        Text(
                            text = "... mais $remaining itens",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    val accruals = (purchase.freight ?: 0.0) + (purchase.tax ?: 0.0)
                    Text(text = "Descontos: ${formatDecimal(purchase.discount)}", style = MaterialTheme.typography.bodySmall)
                    Text(text = "Acréscimos: ${formatDecimal(accruals)}", style = MaterialTheme.typography.bodySmall)
                }
                
                // Total formula: Sum(quantity * price - discount) - purchase.discount + purchase.freight + purchase.tax
                val itemsSum = purchase.items?.sumOf { (it.quantity ?: 0.0) * (it.price ?: 0.0) - (it.discount ?: 0.0) } ?: 0.0
                val total = itemsSum - (purchase.discount ?: 0.0) + (purchase.freight ?: 0.0) + (purchase.tax ?: 0.0)
                
                Text(
                    text = "TOTAL: ${formatDecimal(total)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            purchase.message?.let {
                if (it.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Observações: $it",
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
        }
    }
}

private fun formatDate(dateStr: String?): String {
    if (dateStr == null) return "N/A"
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.US)
        val date = inputFormat.parse(dateStr.take(19))
        if (date != null) outputFormat.format(date) else dateStr
    } catch (e: Exception) {
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.US)
            val date = inputFormat.parse(dateStr)
            if (date != null) outputFormat.format(date) else dateStr
        } catch (e2: Exception) {
            dateStr
        }
    }
}

private fun formatDecimal(value: Double?): String {
    return String.format(Locale.US, "%.2f", value ?: 0.0)
}
