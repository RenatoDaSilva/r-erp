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
import com.r_erp.api.SupabaseService
import com.r_erp.api.SupabaseClient

@Composable
fun ClientsScreen(onClientClick: (Int) -> Unit) {
    var clients by remember { mutableStateOf<List<SupabaseClient>>(emptyList()) }
    var isLoading by remember { mutableStateOf(value = true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val supabaseService = SupabaseService.create()
            clients = supabaseService.getClients()
            isLoading = false
        } catch (e: Exception) {
            errorMessage = e.message ?: "Erro desconhecido"
            isLoading = false
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { onClientClick(-1) }) {
                Icon(Icons.Default.Add, contentDescription = "Novo Cliente")
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
            } else if (clients.isEmpty()) {
                Text(
                    text = "Nenhum cliente encontrado.",
                    modifier = Modifier.align(Alignment.Center),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    items(clients) { client ->
                        ClientItem(client, onClick = { onClientClick(client.id ?: 0) })
                    }
                }
            }
        }
    }
}

@Composable
fun ClientItem(client: SupabaseClient, onClick: () -> Unit) {
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
            Text(text = "ID: ${client.id}", style = MaterialTheme.typography.labelMedium)
            Text(text = client.fullName ?: "Sem Nome", style = MaterialTheme.typography.titleLarge)
            Text(text = "Tel: ${client.phone ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
            client.email?.let {
                Text(text = "E-mail: $it", style = MaterialTheme.typography.bodySmall)
            }
            if (!client.cpf.isNullOrBlank()) {
                Text(text = "CPF: ${client.cpf}", style = MaterialTheme.typography.bodySmall)
            }
            if (!client.city.isNullOrBlank() || !client.state.isNullOrBlank()) {
                val location = listOfNotNull(client.city, client.state).joinToString(", ")
                Text(text = "Local: $location", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
