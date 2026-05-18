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
import com.r_erp.api.SupabaseClient

@Composable
fun SuppliersScreen(onSupplierClick: (Int) -> Unit) {
    var suppliers by remember { mutableStateOf<List<SupabaseClient>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(value = true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val filteredSuppliers = remember(searchQuery, suppliers) {
        if (searchQuery.isBlank()) {
            suppliers
        } else {
            suppliers.filter {
                it.fullName?.contains(searchQuery, ignoreCase = true) == true
            }
        }
    }

    LaunchedEffect(Unit) {
        try {
            val supabaseService = SupabaseService.create()
            suppliers = supabaseService.getSuppliers()
            isLoading = false
        } catch (e: Exception) {
            errorMessage = e.message ?: "Erro desconhecido"
            isLoading = false
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { onSupplierClick(-1) }) {
                Icon(Icons.Default.Add, contentDescription = "Novo Fornecedor")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (!isLoading && errorMessage == null && suppliers.isNotEmpty()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Filtrar por nome...") },
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
                } else if (suppliers.isEmpty()) {
                    Text(
                        text = "Nenhum fornecedor encontrado.",
                        modifier = Modifier.align(Alignment.Center),
                    )
                } else if (filteredSuppliers.isEmpty()) {
                    Text(
                        text = "Nenhum fornecedor corresponde ao filtro.",
                        modifier = Modifier.align(Alignment.Center),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        items(filteredSuppliers) { supplier ->
                            SupplierItem(supplier, onClick = { onSupplierClick(supplier.id ?: 0) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SupplierItem(supplier: SupabaseClient, onClick: () -> Unit) {
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
            Text(text = "ID: ${supplier.id ?: "N/A"}", style = MaterialTheme.typography.labelMedium)
            Text(text = supplier.fullName ?: "Sem Nome", style = MaterialTheme.typography.titleLarge)
            
            supplier.phone?.let {
                Text(text = "Tel: $it", style = MaterialTheme.typography.bodyMedium)
            }
            supplier.email?.let {
                Text(text = "E-mail: $it", style = MaterialTheme.typography.bodyMedium)
            }
            supplier.address?.let {
                Text(text = "Endereço: $it", style = MaterialTheme.typography.bodyMedium)
            }
            if (!supplier.city.isNullOrEmpty() || !supplier.state.isNullOrEmpty()) {
                Text(
                    text = "Local: ${supplier.city ?: ""} - ${supplier.state ?: ""}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            supplier.cpf?.let {
                Text(text = "CPF/CNPJ: $it", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
