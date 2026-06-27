package com.r_erp.ui.screens

import android.widget.Toast
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.r_erp.api.LocalSessionManager
import com.r_erp.api.LocalToken
import com.r_erp.api.SupabaseService
import com.r_erp.api.SupabaseToBuy
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToBuyScreen(onAddToBuy: () -> Unit) {
    val scope = rememberCoroutineScope()
    val token = LocalToken.current
    val sessionManager = LocalSessionManager.current
    val supabaseService = remember(token) { SupabaseService.create(token, sessionManager) }

    var items by remember { mutableStateOf<List<SupabaseToBuy>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun loadData() {
        isLoading = true
        scope.launch {
            try {
                items = supabaseService.getToBuy()
                isLoading = false
            } catch (e: Exception) {
                errorMessage = e.message ?: e.toString()
                isLoading = false
            }
        }
    }

    LaunchedEffect(supabaseService) {
        loadData()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddToBuy) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar a Comprar")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (errorMessage != null) {
                Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center).padding(16.dp))
            } else if (items.isEmpty()) {
                Text(text = "Nada para comprar.", modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    items(items) { item ->
                        ToBuyItem(
                            item = item,
                            onBaixar = {
                                scope.launch {
                                    try {
                                        val newLog = (item.log ?: emptyList()) + "Baixa manual"
                                        val updateData = mapOf(
                                            "check_date" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date()),
                                            "log" to newLog
                                        )
                                        supabaseService.updateToBuy(idFilter = "eq.${item.id}", data = updateData)
                                        // Update local state to hide card
                                        items = items.filter { it.id != item.id }
                                    } catch (e: Exception) {
                                        // Handle error
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ToBuyItem(item: SupabaseToBuy, onBaixar: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }

    Box {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .combinedClickable(
                    onClick = { /* No action on simple click */ },
                    onLongClick = { showMenu = true }
                ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                val productDesc = "${item.productId} - ${item.product?.description ?: "Produto Desconhecido"}"
                Text(text = productDesc, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Quantidade: ${String.format(Locale.US, "%.2f", item.quantity ?: 0.0)}")
                
                val clientName = item.order?.client?.fullName
                if (!clientName.isNullOrBlank()) {
                    Text(text = "Cliente: $clientName", style = MaterialTheme.typography.bodySmall)
                }
                
                item.log?.let { logs ->
                    if (logs.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Log:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        logs.forEach { logItem ->
                            Text(text = logItem, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text = { Text("Baixar") },
                onClick = {
                    showMenu = false
                    onBaixar()
                }
            )
        }
    }
}
