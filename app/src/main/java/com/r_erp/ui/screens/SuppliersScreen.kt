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
import com.r_erp.api.SupabaseSupplier
import kotlinx.coroutines.launch

@Composable
fun SuppliersScreen(onSupplierClick: (Int) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var suppliers by remember { mutableStateOf<List<SupabaseSupplier>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(value = true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val supabaseService = remember { SupabaseService.create() }

    val filteredSuppliers = remember(searchQuery, suppliers) {
        if (searchQuery.isBlank()) {
            suppliers
        } else {
            suppliers.filter {
                it.fullName?.contains(searchQuery, ignoreCase = true) == true
            }
        }
    }

    fun loadSuppliers() {
        isLoading = true
        scope.launch {
            try {
                suppliers = supabaseService.getSuppliers()
                isLoading = false
            } catch (e: Exception) {
                errorMessage = e.message ?: "Erro desconhecido"
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadSuppliers()
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
                            SupplierItem(
                                supplier = supplier,
                                onClick = { onSupplierClick(supplier.id ?: 0) },
                                onDelete = {
                                    scope.launch {
                                        try {
                                            val response = supabaseService.deleteSupplier(idFilter = "eq.${supplier.id}")
                                            if (!response.isSuccessful) throw retrofit2.HttpException(response)
                                            Toast.makeText(context, "Fornecedor excluído", Toast.LENGTH_SHORT).show()
                                            loadSuppliers()
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
fun SupplierItem(supplier: SupabaseSupplier, onClick: () -> Unit, onDelete: () -> Unit) {
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
                supplier.cpfCnpj?.let {
                    Text(text = "CPF/CNPJ: $it", style = MaterialTheme.typography.bodyMedium)
                }
                supplier.pix?.let {
                    if (it.isNotEmpty()) {
                        Text(text = "PIX: $it", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Copiar PIX") },
                onClick = {
                    supplier.pix?.let {
                        clipboardManager.setText(AnnotatedString(it))
                        Toast.makeText(context, "PIX copiado", Toast.LENGTH_SHORT).show()
                    }
                    showMenu = false
                }
            )
            DropdownMenuItem(
                text = { Text("Copiar telefone") },
                onClick = {
                    supplier.phone?.let {
                        clipboardManager.setText(AnnotatedString(it))
                        Toast.makeText(context, "Telefone copiado", Toast.LENGTH_SHORT).show()
                    }
                    showMenu = false
                }
            )
            DropdownMenuItem(
                text = { Text("Copiar Nome") },
                onClick = {
                    supplier.fullName?.let {
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
