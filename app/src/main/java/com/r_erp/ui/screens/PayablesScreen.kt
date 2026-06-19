package com.r_erp.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.r_erp.api.SupabaseSupplier
import com.r_erp.api.SupabasePayable
import com.r_erp.api.SupabasePayableTotal
import com.r_erp.api.SupabaseService
import com.r_erp.api.LocalToken
import com.r_erp.api.LocalSessionManager
import com.r_erp.utils.PdfUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayablesScreen(onAddPayable: () -> Unit, onPayableClick: (Int) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val token = LocalToken.current
    val sessionManager = LocalSessionManager.current
    val supabaseService = remember(token) { SupabaseService.create(token, sessionManager) }
    
    var payables by remember { mutableStateOf<List<SupabasePayable>>(emptyList()) }
    var suppliers by remember { mutableStateOf<List<SupabaseSupplier>>(emptyList()) }
    var totals by remember { mutableStateOf<SupabasePayableTotal?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(value = true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Filters
    var showOpen by remember { mutableStateOf(true) }
    var showPaid by remember { mutableStateOf(true) }
    
    // Dialog States
    var showBaixarDialog by remember { mutableStateOf<SupabasePayable?>(null) }
    var showParcelarDialog by remember { mutableStateOf<SupabasePayable?>(null) }
    var showDatePickerFor by remember { mutableStateOf<SupabasePayable?>(null) }
    var paidValueInput by remember { mutableStateOf("") }
    
    // Split States
    var splitEntryFee by remember { mutableStateOf("0.00") }
    var splitInstallments by remember { mutableIntStateOf(1) }
    var splitFirstDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())) }
    var showSplitDatePicker by remember { mutableStateOf(false) }

    val supplierMap = remember(suppliers) { suppliers.associate { it.id to it.fullName } }

    val filteredPayables = remember(searchQuery, payables, showOpen, showPaid, supplierMap) {
        payables.filter {
            val supplierName = supplierMap[it.supplierId] ?: ""
            val matchesSearch = supplierName.contains(searchQuery, ignoreCase = true) || 
                               (it.origin?.contains(searchQuery, ignoreCase = true) ?: false)
            val matchesStatus = (showOpen && it.paidAt == null) || (showPaid && it.paidAt != null)
            matchesSearch && matchesStatus
        }
    }

    fun loadData() {
        isLoading = true
        scope.launch {
            try {
                suppliers = supabaseService.getSuppliers()
                payables = supabaseService.getPayables()
                val totalsList = supabaseService.getPayablesTotals()
                if (totalsList.isNotEmpty()) totals = totalsList[0]
                isLoading = false
            } catch (e: Exception) {
                if (e.message?.contains("composition") != true) {
                    errorMessage = e.message ?: e.toString()
                }
                isLoading = false
            }
        }
    }

    LaunchedEffect(supabaseService) {
        loadData()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddPayable) {
                Icon(Icons.Default.Add, contentDescription = "Novo Pagamento")
            }
        },
        bottomBar = {
            if (totals != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Total a Pagar", style = MaterialTheme.typography.labelSmall)
                            Text(text = formatDecimal(totals?.outstanding), fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "Total Pago", style = MaterialTheme.typography.labelSmall)
                            Text(text = formatDecimal(totals?.paid), fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header with search and checkboxes
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Filtrar por fornecedor...") },
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
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = showOpen, onCheckedChange = { showOpen = it })
                    Text("Em aberto")
                    Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                    Checkbox(checked = showPaid, onCheckedChange = { showPaid = it })
                    Text("Pagos")
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (errorMessage != null) {
                    Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center))
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
                    ) {
                        items(filteredPayables) { payable ->
                            PayableItem(
                                payable = payable,
                                supplierName = supplierMap[payable.supplierId] ?: "N/A",
                                onClick = { if (payable.paidAt == null) onPayableClick(payable.id ?: 0) },
                                onBaixar = {
                                    showBaixarDialog = payable
                                    paidValueInput = String.format(Locale.US, "%.2f", payable.value ?: 0.0)
                                },
                                onDateLongClick = {
                                    if (payable.paidAt == null) {
                                        showDatePickerFor = payable
                                    }
                                },
                                onParcelar = {
                                    showParcelarDialog = payable
                                    splitEntryFee = "0.00"
                                    splitInstallments = 1
                                    splitFirstDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                                },
                                onPrint = {
                                    PdfUtils.generateAndSharePayablesReport(context, filteredPayables, supplierMap, totals)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showBaixarDialog != null) {
        AlertDialog(
            onDismissRequest = { showBaixarDialog = null },
            title = { Text("Informe o valor pago") },
            text = {
                Column {
                    OutlinedTextField(
                        value = paidValueInput,
                        onValueChange = { paidValueInput = it },
                        label = { Text("Valor Pago") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val valInput = paidValueInput.toDoubleOrNull() ?: 0.0
                    val pay = showBaixarDialog!!
                    scope.launch {
                        try {
                            val updateMap = mapOf(
                                "paid_value" to valInput,
                                "paid_at" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date())
                            )
                            supabaseService.updatePayable(idFilter = "eq.${pay.id}", payable = updateMap)
                            showBaixarDialog = null
                            loadData()
                            Toast.makeText(context, "Baixa realizada com sucesso!", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Erro ao baixar: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBaixarDialog = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showParcelarDialog != null) {
        val pay = showParcelarDialog!!
        val entry = splitEntryFee.toDoubleOrNull() ?: 0.0
        val installmentValue = ((pay.value ?: 0.0) - entry) / splitInstallments

        AlertDialog(
            onDismissRequest = { showParcelarDialog = null },
            title = { Text("Parcelar Pagamento") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = splitEntryFee,
                        onValueChange = { splitEntryFee = it },
                        label = { Text("Entrada:") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Parcelas:")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (splitInstallments > 1) splitInstallments-- }) {
                                Icon(Icons.Default.Remove, contentDescription = "Diminuir")
                            }
                            Text(text = splitInstallments.toString(), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                            IconButton(onClick = { splitInstallments++ }) {
                                Icon(Icons.Default.Add, contentDescription = "Aumentar")
                            }
                        }
                    }

                    OutlinedTextField(
                        value = try {
                            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                            val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.US)
                            formatter.format(parser.parse(splitFirstDate)!!)
                        } catch (e: Exception) { splitFirstDate },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Primeira parcela") },
                        trailingIcon = {
                            IconButton(onClick = { showSplitDatePicker = true }) {
                                Icon(Icons.Default.CalendarToday, contentDescription = "Selecionar data")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "Valor da parcela: ${String.format(Locale.forLanguageTag("pt-BR"), "%.2f", installmentValue)}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        try {
                            val response = supabaseService.splitPayable(
                                mapOf(
                                    "p_id" to (pay.id ?: 0),
                                    "p_entry_fee" to entry,
                                    "p_installment_count" to splitInstallments,
                                    "p_first_installment_date" to splitFirstDate
                                )
                            )
                            if (response.isSuccessful) {
                                showParcelarDialog = null
                                loadData()
                                Toast.makeText(context, "Parcelamento realizado com sucesso!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Erro: ${response.errorBody()?.string()}", Toast.LENGTH_LONG).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Erro ao parcelar: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showParcelarDialog = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showSplitDatePicker) {
        val initialDate = try {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(splitFirstDate)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }

        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDate)

        DatePickerDialog(
            onDismissRequest = { showSplitDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        splitFirstDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(it))
                    }
                    showSplitDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showSplitDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showDatePickerFor != null) {
        val pay = showDatePickerFor!!
        val initialDate = try {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(pay.dueDate?.take(10) ?: "")?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialDate,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    // Normalize current time to start of day in UTC for comparison
                    val calendar = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
                        set(java.util.Calendar.HOUR_OF_DAY, 0)
                        set(java.util.Calendar.MINUTE, 0)
                        set(java.util.Calendar.SECOND, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                    }
                    return utcTimeMillis >= calendar.timeInMillis
                }
            }
        )

        DatePickerDialog(
            onDismissRequest = { showDatePickerFor = null },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { newDateMillis ->
                        scope.launch {
                            try {
                                val apiDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                                val newDateStr = apiDateFormat.format(Date(newDateMillis))
                                supabaseService.updatePayable(idFilter = "eq.${pay.id}", payable = mapOf("due_date" to newDateStr))
                                loadData()
                                Toast.makeText(context, "Data de vencimento atualizada!", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Erro ao atualizar: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                    showDatePickerFor = null
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerFor = null }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PayableItem(
    payable: SupabasePayable,
    supplierName: String,
    onClick: () -> Unit,
    onBaixar: () -> Unit,
    onDateLongClick: () -> Unit,
    onParcelar: () -> Unit,
    onPrint: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val cardBorder = if (payable.paidAt == null) {
        BorderStroke(2.dp, Color(0xFFFF5722)) // Different color for payables
    } else {
        null
    }

    Box {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showMenu = true }
                ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = cardBorder
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = supplierName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(text = formatDecimal(payable.value), fontWeight = FontWeight.Bold)
                }
                
                payable.origin?.let {
                    Text(text = it, style = MaterialTheme.typography.bodySmall)
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = "Vencimento: ${formatDate(payable.dueDate)}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.combinedClickable(
                            onClick = {}, 
                            onLongClick = onDateLongClick
                        )
                    )
                    if (payable.paidAt != null) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "Pago em: ${formatDate(payable.paidAt)}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF4CAF50))
                            Text(text = "Valor Pago: ${formatDecimal(payable.paidValue)}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF4CAF50))
                        }
                    } else {
                        Text(text = "EM ABERTO", style = MaterialTheme.typography.bodySmall, color = Color(0xFFFF5722), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            val isNotPaid = payable.paidAt == null || payable.paidAt == ""
            if (isNotPaid) {
                DropdownMenuItem(text = { Text("Baixar") }, onClick = { showMenu = false; onBaixar() })
            }
            DropdownMenuItem(
                text = { Text("Parcelar...") }, 
                onClick = { showMenu = false; onParcelar() },
                enabled = isNotPaid
            )
            DropdownMenuItem(text = { Text("Imprimir") }, onClick = { showMenu = false; onPrint() })
        }
    }
}

private fun formatDate(dateStr: String?): String {
    if (dateStr == null) return "N/A"
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.US)
        val date = inputFormat.parse(dateStr.take(10))
        if (date != null) outputFormat.format(date) else dateStr
    } catch (e: Exception) {
        dateStr
    }
}

private fun formatDecimal(value: Double?): String {
    return String.format(Locale.forLanguageTag("pt-BR"), "%.2f", value ?: 0.0)
}
