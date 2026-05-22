package com.r_erp.ui.screens

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.r_erp.api.SupabaseService
import com.r_erp.api.SupabaseClient
import kotlinx.coroutines.launch

@Composable
fun ClientsScreen(onClientClick: (Int) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var clients by remember { mutableStateOf<List<SupabaseClient>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(value = true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val supabaseService = remember { SupabaseService.create() }

    val filteredClients = remember(searchQuery, clients) {
        if (searchQuery.isBlank()) {
            clients
        } else {
            clients.filter {
                it.fullName?.contains(searchQuery, ignoreCase = true) == true
            }
        }
    }

    fun loadClients() {
        isLoading = true
        scope.launch {
            try {
                val fetchedClients = supabaseService.getClients()
                clients = fetchedClients.sortedBy { it.fullName?.lowercase() ?: "" }
                isLoading = false
            } catch (e: Exception) {
                errorMessage = e.message ?: "Erro desconhecido"
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadClients()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { onClientClick(-1) }) {
                Icon(Icons.Default.Add, contentDescription = "Novo Cliente")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (!isLoading && errorMessage == null && clients.isNotEmpty()) {
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
                } else if (clients.isEmpty()) {
                    Text(
                        text = "Nenhum cliente encontrado.",
                        modifier = Modifier.align(Alignment.Center),
                    )
                } else if (filteredClients.isEmpty()) {
                    Text(
                        text = "Nenhum cliente corresponde ao filtro.",
                        modifier = Modifier.align(Alignment.Center),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        items(filteredClients) { client ->
                            ClientItem(
                                client = client,
                                onClick = { onClientClick(client.id ?: 0) },
                                onDelete = {
                                    scope.launch {
                                        try {
                                            val response = supabaseService.deleteClient(idFilter = "eq.${client.id}")
                                            if (!response.isSuccessful) throw retrofit2.HttpException(response)
                                            Toast.makeText(context, "Cliente excluído", Toast.LENGTH_SHORT).show()
                                            loadClients()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Erro ao excluir: ${e.message}", Toast.LENGTH_LONG).show()
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
fun ClientItem(client: SupabaseClient, onClick: () -> Unit, onDelete: () -> Unit) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
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

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Copiar telefone") },
                onClick = {
                    client.phone?.let {
                        clipboardManager.setText(AnnotatedString(it))
                        Toast.makeText(context, "Telefone copiado", Toast.LENGTH_SHORT).show()
                    }
                    showMenu = false
                }
            )
            DropdownMenuItem(
                text = { Text("Copiar Nome") },
                onClick = {
                    client.fullName?.let {
                        clipboardManager.setText(AnnotatedString(it))
                        Toast.makeText(context, "Nome copiado", Toast.LENGTH_SHORT).show()
                    }
                    showMenu = false
                }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Excluir ...") },
                onClick = {
                    showMenu = false
                    onDelete()
                }
            )
        }
    }
}
