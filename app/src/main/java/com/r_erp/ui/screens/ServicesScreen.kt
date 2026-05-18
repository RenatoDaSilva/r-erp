package com.r_erp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.unit.dp
import com.r_erp.api.SupabaseService
import com.r_erp.api.SupabaseServiceItem
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ServicesScreen(onServiceClick: (Int) -> Unit) {
    var services by remember { mutableStateOf<List<SupabaseServiceItem>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(value = true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val filteredServices = remember(searchQuery, services) {
        if (searchQuery.isBlank()) {
            services
        } else {
            services.filter {
                it.description?.contains(searchQuery, ignoreCase = true) == true
            }
        }
    }

    LaunchedEffect(Unit) {
        try {
            val supabaseService = SupabaseService.create()
            services = supabaseService.getServices()
            isLoading = false
        } catch (e: Exception) {
            errorMessage = e.message ?: e.toString()
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
            if (!isLoading && errorMessage == null && services.isNotEmpty()) {
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
                } else if (services.isEmpty()) {
                    Text(
                        text = "Nenhum serviço encontrado.",
                        modifier = Modifier.align(Alignment.Center),
                    )
                } else if (filteredServices.isEmpty()) {
                    Text(
                        text = "Nenhum serviço corresponde ao filtro.",
                        modifier = Modifier.align(Alignment.Center),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        items(filteredServices) { service ->
                            ServiceItem(service, onClick = { onServiceClick(service.id ?: 0) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ServiceItem(service: SupabaseServiceItem, onClick: () -> Unit) {
    val localeBR = Locale("pt", "BR")
    val currencyFormatter = NumberFormat.getCurrencyInstance(localeBR)
    
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
                text = "Preço: ${currencyFormatter.format(service.price ?: 0.0)}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
