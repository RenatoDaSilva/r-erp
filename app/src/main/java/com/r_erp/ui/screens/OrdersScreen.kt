package com.r_erp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.r_erp.api.SupabaseOrder
import com.r_erp.api.SupabaseOrderItem
import com.r_erp.api.SupabaseService
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@Composable
fun OrdersScreen(onAddOrder: () -> Unit, onOrderClick: (Int) -> Unit) {
    var orders by remember { mutableStateOf<List<SupabaseOrder>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(value = true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val filteredOrders = remember(searchQuery, orders) {
        if (searchQuery.isBlank()) {
            orders
        } else {
            orders.filter {
                it.clientName?.contains(searchQuery, ignoreCase = true) == true
            }
        }
    }

    LaunchedEffect(Unit) {
        try {
            val supabaseService = SupabaseService.create()
            orders = supabaseService.getOrdersWithItems()
            isLoading = false
        } catch (e: Exception) {
            errorMessage = e.message ?: e.toString()
            isLoading = false
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddOrder) {
                Icon(Icons.Default.Add, contentDescription = "Novo Pedido")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (!isLoading && errorMessage == null && orders.isNotEmpty()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Filtrar por cliente...") },
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
                } else if (orders.isEmpty()) {
                    Text(
                        text = "Nenhum pedido encontrado.",
                        modifier = Modifier.align(Alignment.Center),
                    )
                } else if (filteredOrders.isEmpty()) {
                    Text(
                        text = "Nenhum pedido corresponde ao filtro.",
                        modifier = Modifier.align(Alignment.Center),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        items(filteredOrders) { order ->
                            OrderItem(order, onClick = { onOrderClick(order.id ?: 0) })
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OrderItem(order: SupabaseOrder, onClick: () -> Unit) {
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
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = formatDate(order.createdAt),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                
                Text(
                    text = order.clientName ?: "Cliente não informado",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Items table
                order.items?.let { items ->
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
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text(text = item.description ?: "", modifier = Modifier.weight(3f), fontSize = 10.sp, maxLines = 1)
                                Text(text = formatDecimal(item.quantity), modifier = Modifier.weight(1f), fontSize = 10.sp)
                                Text(text = formatDecimal(item.price), modifier = Modifier.weight(1.5f), fontSize = 10.sp)
                                Text(text = formatDecimal(item.discount), modifier = Modifier.weight(1.5f), fontSize = 10.sp)
                                Text(text = formatDecimal(item.total), modifier = Modifier.weight(1.5f), fontSize = 10.sp)
                            }
                        }

                        if (items.size > 3) {
                            val remaining = (order.itemsCount ?: items.size) - 3
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
                        Text(text = "Total Itens: ${formatDecimal(order.totalItems)}", style = MaterialTheme.typography.bodySmall)
                        Text(text = "Desconto: ${formatDecimal(order.discount)}", style = MaterialTheme.typography.bodySmall)
                    }
                    Text(
                        text = "TOTAL: ${formatDecimal(order.total)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                order.message?.let {
                    if (it.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Gerar PDF ...") },
                onClick = {
                    showMenu = false
                    // Action to be applied later
                }
            )
            DropdownMenuItem(
                text = { Text("Enviar por Whatsapp ...") },
                onClick = {
                    showMenu = false
                    // Action to be applied later
                }
            )
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
