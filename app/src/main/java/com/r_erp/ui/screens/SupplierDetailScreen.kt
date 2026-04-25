package com.r_erp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.widget.Toast
import com.r_erp.api.ApiService
import com.r_erp.api.Supplier
import kotlinx.coroutines.launch

@Composable
fun SupplierDetailScreen(supplierId: Int, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(value = supplierId != -1) }
    var isSaving by remember { mutableStateOf(value = false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Editable states
    var fullname by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var cpf by remember { mutableStateOf("") }

    LaunchedEffect(supplierId) {
        if (supplierId != -1) {
            try {
                val apiService = ApiService.create()
                val fetchedSupplier = apiService.getSupplier(id = supplierId)

                // Initialize states
                fullname = fetchedSupplier.fullname
                phone = fetchedSupplier.phone
                email = fetchedSupplier.email ?: ""
                address = fetchedSupplier.address ?: ""
                city = fetchedSupplier.city ?: ""
                state = fetchedSupplier.state ?: ""
                cpf = fetchedSupplier.cpf ?: ""

                isLoading = false
            } catch (e: Exception) {
                errorMessage = e.message ?: "Erro ao carregar dados"
                isLoading = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (errorMessage != null) {
            Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp),
                )
                Button(onClick = onBack) {
                    Text("Voltar")
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (supplierId == -1) {
                    Text(text = "Novo Fornecedor", style = MaterialTheme.typography.headlineSmall)
                } else {
                    Text(text = "Editar Fornecedor ID: $supplierId", style = MaterialTheme.typography.headlineSmall)
                }
                
                OutlinedTextField(
                    value = fullname,
                    onValueChange = { fullname = it },
                    label = { Text("Nome Completo / Razão Social") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Telefone") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("E-mail") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Endereço") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("Cidade") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = state,
                    onValueChange = { state = it },
                    label = { Text("Estado") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = cpf,
                    onValueChange = { cpf = it },
                    label = { Text("CPF / CNPJ") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        isSaving = true
                        scope.launch {
                            try {
                                val apiService = ApiService.create()
                                val supplierToUpdate = Supplier(
                                    id = supplierId,
                                    fullname = fullname,
                                    phone = phone,
                                    email = email,
                                    address = address,
                                    city = city,
                                    state = state,
                                    cpf = cpf
                                )
                                apiService.updateSupplier(supplier = supplierToUpdate)
                                Toast.makeText(context, "Fornecedor salvo com sucesso!", Toast.LENGTH_SHORT).show()
                                onBack()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Erro ao salvar: ${e.message}", Toast.LENGTH_LONG).show()
                            } finally {
                                isSaving = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = 8.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Text("Salvar")
                }
                
                TextButton(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancelar")
                }
            }
        }
    }
}
