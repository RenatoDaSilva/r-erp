package com.r_erp.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import android.util.Log
import com.r_erp.api.SupabaseSupplier
import com.r_erp.api.SupabaseService
import com.r_erp.api.SupabasePayable
import com.r_erp.api.LocalToken
import com.r_erp.api.LocalSessionManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayableDetailsScreen(payableId: Int, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val token = LocalToken.current
    val sessionManager = LocalSessionManager.current
    val supabaseService = remember(token) { SupabaseService.create(token, sessionManager) }

    var isLoading by remember { mutableStateOf(value = payableId != -1) }
    var isSaving by remember { mutableStateOf(value = false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var errorDialogMessage by remember { mutableStateOf<String?>(null) }

    var isInitialized by remember { mutableStateOf(false) }

    // Editable states
    var selectedSupplier by remember { mutableStateOf<SupabaseSupplier?>(null) }
    var origin by remember { mutableStateOf("") }
    var purchaseIdStored by remember { mutableStateOf<Int?>(null) }
    var valueStr by remember { mutableStateOf("") }
    var dueAtMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    
    // Options lists
    var suppliers by remember { mutableStateOf<List<SupabaseSupplier>>(emptyList()) }
    var supplierExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") } }

    LaunchedEffect(supabaseService) {
        try {
            suppliers = supabaseService.getSuppliers().sortedBy { it.fullName?.lowercase() ?: "" }
            
            if (!isInitialized) {
                if (payableId != -1) {
                    val fetched = supabaseService.getPayable(idFilter = "eq.$payableId")
                    if (fetched.isNotEmpty()) {
                        val pay = fetched[0]
                        selectedSupplier = suppliers.find { it.id == pay.supplierId }
                        origin = pay.origin ?: ""
                        purchaseIdStored = pay.purchaseId
                        valueStr = String.format(Locale.US, "%.2f", pay.value ?: 0.0)
                        
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                        pay.dueDate?.let {
                            dueAtMillis = sdf.parse(it.take(10))?.time ?: dueAtMillis
                        }
                    } else {
                        errorMessage = "Título não encontrado"
                    }
                } else {
                    valueStr = "0.00"
                }
                isInitialized = true
            }
            isLoading = false
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            val msg = e.message ?: e.toString()
            if (!msg.contains("composition", ignoreCase = true)) {
                errorMessage = msg
            }
            isLoading = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (errorMessage != null) {
            Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
                Button(onClick = onBack) { Text("Voltar") }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = if (payableId == -1) "Novo Pagamento" else "Editar Título ID: $payableId", style = MaterialTheme.typography.headlineSmall)

                // Supplier selection
                ExposedDropdownMenuBox(
                    expanded = supplierExpanded,
                    onExpandedChange = { supplierExpanded = !supplierExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedSupplier?.fullName ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Fornecedor") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = supplierExpanded) },
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = supplierExpanded, onDismissRequest = { supplierExpanded = false }) {
                        suppliers.forEach { supplier ->
                            DropdownMenuItem(
                                text = { Text(supplier.fullName ?: "") },
                                onClick = {
                                    selectedSupplier = supplier
                                    supplierExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = valueStr,
                    onValueChange = { valueStr = it },
                    label = { Text("Valor") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Read-only info fields
                if (origin.isNotBlank()) {
                    OutlinedTextField(
                        value = origin,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Origem") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (purchaseIdStored != null) {
                    OutlinedTextField(
                        value = purchaseIdStored.toString(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Compra ID") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Due Date
                OutlinedTextField(
                    value = dateFormatter.format(Date(dueAtMillis)),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Data de Vencimento") },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Selecionar Data")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (selectedSupplier == null) {
                            Toast.makeText(context, "Selecione um fornecedor", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isSaving = true
                        scope.launch {
                            try {
                                val apiDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                                val dataMap = mutableMapOf<String, Any>()
                                dataMap["supplier_id"] = selectedSupplier!!.id!!
                                if (origin.isNotBlank()) dataMap["origin"] = origin
                                dataMap["value"] = valueStr.toDoubleOrNull() ?: 0.0
                                dataMap["due_date"] = apiDateFormat.format(Date(dueAtMillis))

                                if (payableId == -1) {
                                    val response = supabaseService.createPayable(dataMap)
                                    if (!response.isSuccessful) throw retrofit2.HttpException(response)
                                } else {
                                    val response = supabaseService.updatePayable(idFilter = "eq.$payableId", payable = dataMap)
                                    if (!response.isSuccessful) throw retrofit2.HttpException(response)
                                }

                                Toast.makeText(context, "Título salvo com sucesso!", Toast.LENGTH_SHORT).show()
                                onBack()
                            } catch (e: Exception) {
                                val errorBody = (e as? retrofit2.HttpException)?.response()?.errorBody()?.string()
                                Log.e("PayableDetails", "Error saving payable: $errorBody", e)
                                val msg = if (e is retrofit2.HttpException) {
                                    "Erro ${e.code()}: ${errorBody ?: e.message()}"
                                } else {
                                    e.message ?: "Erro desconhecido"
                                }
                                errorDialogMessage = msg
                            } finally {
                                isSaving = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving
                ) {
                    if (isSaving) CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    Text("Salvar")
                }
                
                TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Cancelar") }
            }
        }
    }

    if (errorDialogMessage != null) {
        AlertDialog(
            onDismissRequest = { errorDialogMessage = null },
            title = { Text("Erro ao Salvar") },
            text = { Text(errorDialogMessage!!) },
            confirmButton = {
                TextButton(onClick = { errorDialogMessage = null }) {
                    Text("OK")
                }
            }
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dueAtMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { dueAtMillis = it }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) { DatePicker(state = datePickerState) }
    }
}
