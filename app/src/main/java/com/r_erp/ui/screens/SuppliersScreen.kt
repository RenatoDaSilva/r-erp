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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.r_erp.api.ApiService
import com.r_erp.api.Supplier

@Composable
fun SuppliersScreen(onSupplierClick: (Int) -> Unit) {
    var suppliers by remember { mutableStateOf<List<Supplier>>(emptyList()) }
    var isLoading by remember { mutableStateOf(value = true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val apiService = ApiService.create()
            suppliers = apiService.getSuppliers(option = "fornecedores")
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
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
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    items(suppliers) { supplier ->
                        SupplierItem(supplier, onClick = { onSupplierClick(supplier.id) })
                    }
                }
            }
        }
    }
}

@Composable
fun SupplierItem(supplier: Supplier, onClick: () -> Unit) {
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
            Text(text = "ID: ${supplier.id}", style = MaterialTheme.typography.labelMedium)
            Text(text = supplier.fullname, style = MaterialTheme.typography.titleLarge)
            Text(text = "Tel: ${supplier.phone}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
