package com.r_erp.ui.screens

import android.widget.Toast
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
import com.r_erp.api.SupabaseService
import com.r_erp.api.SupabaseServiceItem
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun ServiceDetailScreen(serviceId: Int, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(value = true) }
    var isSaving by remember { mutableStateOf(value = false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Editable states
    var description by remember { mutableStateOf("") }
    var priceStr by remember { mutableStateOf("") }

    LaunchedEffect(serviceId) {
        try {
            val supabaseService = SupabaseService.create()
            
            if (serviceId != -1) {
                val fetchedServices = supabaseService.getService(idFilter = "eq.$serviceId")
                if (fetchedServices.isNotEmpty()) {
                    val fetchedService = fetchedServices[0]
                    description = fetchedService.description ?: ""
                    priceStr = String.format(Locale.US, "%.2f", fetchedService.price ?: 0.0)
                } else {
                    errorMessage = "Serviço não encontrado"
                }
            } else {
                priceStr = "0.00"
            }
            isLoading = false
        } catch (e: Exception) {
            errorMessage = e.message ?: "Erro ao carregar dados"
            isLoading = false
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
                if (serviceId == -1) {
                    Text(text = "Novo Serviço", style = MaterialTheme.typography.headlineSmall)
                } else {
                    Text(text = "Editar Serviço ID: $serviceId", style = MaterialTheme.typography.headlineSmall)
                }

                if (serviceId != -1) {
                    OutlinedTextField(
                        value = serviceId.toString(),
                        onValueChange = {},
                        label = { Text("ID") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true
                    )
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrição") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = priceStr,
                    onValueChange = { priceStr = it },
                    label = { Text("Preço") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        isSaving = true
                        scope.launch {
                            try {
                                val supabaseService = SupabaseService.create()
                                val serviceToSave = SupabaseServiceItem(
                                    description = description,
                                    price = priceStr.toDoubleOrNull() ?: 0.0
                                )
                                
                                if (serviceId == -1) {
                                    supabaseService.createService(service = serviceToSave)
                                } else {
                                    supabaseService.updateService(
                                        idFilter = "eq.$serviceId",
                                        service = serviceToSave
                                    )
                                }
                                
                                Toast.makeText(context, "Serviço salvo com sucesso!", Toast.LENGTH_SHORT).show()
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
