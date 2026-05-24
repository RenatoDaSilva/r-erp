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
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.r_erp.api.SupabaseClient
import com.r_erp.api.SupabaseBudget
import com.r_erp.api.SupabaseBudgetItem
import com.r_erp.api.SupabaseService
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

import androidx.compose.ui.platform.LocalContext
import com.r_erp.utils.PdfUtils
import android.widget.Toast
import kotlinx.coroutines.launch

@Composable
fun BudgetsScreen(onAddBudget: () -> Unit, onBudgetClick: (Int) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val supabaseService = remember { SupabaseService.create() }
    var budgets by remember { mutableStateOf<List<SupabaseBudget>>(emptyList()) }
    var clients by remember { mutableStateOf<List<SupabaseClient>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(value = true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val listState = rememberLazyListState()
    var lastProcessedBudgetId by remember { mutableStateOf<Int?>(null) }

    val clientMap = remember(clients) { clients.associate { it.id to it.fullName } }

    val filteredBudgets = remember(searchQuery, budgets, clientMap) {
        if (searchQuery.isBlank()) {
            budgets
        } else {
            budgets.filter {
                val clientName = it.clientName ?: clientMap[it.clientId] ?: ""
                clientName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    fun loadBudgets() {
        isLoading = true
        scope.launch {
            try {
                clients = supabaseService.getClients()
                budgets = supabaseService.getBudgetsWithItems()
                isLoading = false
            } catch (e: Exception) {
                errorMessage = e.message ?: e.toString()
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadBudgets()
    }

    LaunchedEffect(budgets) {
        if (lastProcessedBudgetId != null && budgets.isNotEmpty()) {
            val index = filteredBudgets.indexOfFirst { it.id == lastProcessedBudgetId }
            if (index != -1) {
                listState.scrollToItem(index)
            }
            lastProcessedBudgetId = null
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddBudget) {
                Icon(Icons.Default.Add, contentDescription = "Novo Orçamento")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (!isLoading && errorMessage == null && budgets.isNotEmpty()) {
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
                } else if (budgets.isEmpty()) {
                    Text(
                        text = "Nenhum orçamento encontrado.",
                        modifier = Modifier.align(Alignment.Center),
                    )
                } else if (filteredBudgets.isEmpty()) {
                    Text(
                        text = "Nenhum orçamento corresponde ao filtro.",
                        modifier = Modifier.align(Alignment.Center),
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        items(filteredBudgets) { budget ->
                            BudgetItem(
                                budget, 
                                clientName = budget.clientName ?: clientMap[budget.clientId] ?: "N/A",
                                onClick = { onBudgetClick(budget.id ?: 0) },
                                onCloseOrder = {
                                    scope.launch {
                                        try {
                                            val result = supabaseService.createOrderFromBudget(mapOf("budget_id" to (budget.id ?: 0)))
                                            if (result == -1) {
                                                Toast.makeText(context, "Já existe um pedido gerado para este orçamento.", Toast.LENGTH_LONG).show()
                                            } else {
                                                Toast.makeText(context, "Pedido $result gerado com sucesso!", Toast.LENGTH_LONG).show()
                                                lastProcessedBudgetId = budget.id
                                                loadBudgets()
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Erro ao fechar pedido: ${e.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
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
fun BudgetItem(budget: SupabaseBudget, clientName: String, onClick: () -> Unit, onCloseOrder: () -> Unit) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    val highlightColor = MaterialTheme.colorScheme.primary
    val cardBorder = if (budget.orderId == null) {
        BorderStroke(2.dp, highlightColor)
    } else {
        null
    }

    Box {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .combinedClickable(
                    onClick = { if (budget.orderId == null) onClick() },
                    onLongClick = { showMenu = true }
                ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = cardBorder
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = formatDate(budget.createdAt),
                        style = MaterialTheme.typography.labelMedium
                    )
                    if (budget.orderId != null) {
                        Text(
                            text = "Pedido: ${budget.orderId}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                
                Text(
                    text = clientName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = "Validade: ${formatDate(budget.validUntil)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Items table
                budget.items?.let { items ->
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
                            val remaining = (budget.itemsCount ?: items.size) - 3
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
                        Text(text = "Total Itens: ${formatDecimal(budget.totalItems)}", style = MaterialTheme.typography.bodySmall)
                        Text(text = "Desconto: ${formatDecimal(budget.discount)}", style = MaterialTheme.typography.bodySmall)
                    }
                    Text(
                        text = "TOTAL: ${formatDecimal(budget.total)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                budget.message?.let {
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
                    PdfUtils.generateAndShareBudgetPdf(context, budget, clientName = clientName)
                }
            )
            DropdownMenuItem(
                text = { Text("Enviar por Whatsapp ...") },
                onClick = {
                    showMenu = false
                    PdfUtils.generateAndShareBudgetPdf(context, budget, clientName = clientName, viaWhatsapp = true)
                }
            )
            DropdownMenuItem(
                text = { Text("Fechar pedido ...") },
                onClick = {
                    showMenu = false
                    onCloseOrder()
                },
                enabled = budget.orderId == null
            )
        }
    }
}

private fun formatDate(dateStr: String?): String {
    if (dateStr == null) return "N/A"
    return try {
        // Handle various date formats if necessary, assuming ISO or similar
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.US)
        val date = inputFormat.parse(dateStr.take(19)) // Take date and time part
        if (date != null) outputFormat.format(date) else dateStr
    } catch (e: Exception) {
        // Fallback for simple yyyy-MM-dd
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
