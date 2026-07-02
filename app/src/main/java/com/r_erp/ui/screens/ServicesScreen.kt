package com.r_erp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.r_erp.api.SupabaseService
import com.r_erp.api.SupabaseServiceItem
import com.r_erp.api.LocalToken
import com.r_erp.api.LocalSessionManager
import java.util.Locale

@Composable
fun ServicesScreen(onServiceClick: (Int) -> Unit) {
    val token = LocalToken.current
    val sessionManager = LocalSessionManager.current
    val focusManager = LocalFocusManager.current
    var services by remember { mutableStateOf<List<SupabaseServiceItem>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(value = true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val supabaseService = remember(token) { SupabaseService.create(token, sessionManager) }

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
                                ServiceItem(service, onClick = { 
                                    focusManager.clearFocus()
                                    onServiceClick(service.id ?: 0) 
                                })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ServiceItem(service: SupabaseServiceItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() },
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
}
