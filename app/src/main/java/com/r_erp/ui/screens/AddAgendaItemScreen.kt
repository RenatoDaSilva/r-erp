package com.r_erp.ui.screens

import android.app.TimePickerDialog
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.r_erp.api.ApiService
import com.r_erp.api.AgendaItem
import com.r_erp.api.Client
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAgendaItemScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val apiService = remember { ApiService.create() }

    var clients by remember { mutableStateOf<List<Client>>(emptyList()) }
    var isLoadingClients by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    // Form states
    var selectedClientName by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var description by remember { mutableStateOf("") }
    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var startHour by remember { mutableStateOf(8) }
    var startMinute by remember { mutableStateOf(0) }
    var endHour by remember { mutableStateOf(9) }
    var endMinute by remember { mutableStateOf(0) }
    var fullDay by remember { mutableStateOf(false) }

    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") } }
    val timeFormat = "%02d:%02d"

    LaunchedEffect(Unit) {
        try {
            clients = apiService.getClients()
            isLoadingClients = false
        } catch (e: Exception) {
            Toast.makeText(context, "Erro ao carregar clientes: ${e.message}", Toast.LENGTH_LONG).show()
            isLoadingClients = false
        }
    }

    if (isLoadingClients) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "Novo Agendamento", style = MaterialTheme.typography.headlineSmall)

            // Client Dropdown
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedClientName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Cliente") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    clients.forEach { client ->
                        DropdownMenuItem(
                            text = { Text(client.fullname) },
                            onClick = {
                                selectedClientName = client.fullname
                                expanded = false
                            }
                        )
                    }
                }
            }

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descrição") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            // Date Picker
            OutlinedTextField(
                value = dateFormatter.format(Date(selectedDateMillis)),
                onValueChange = {},
                readOnly = true,
                label = { Text("Data") },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Selecionar Data")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            if (!fullDay) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Start Time
                    OutlinedTextField(
                        value = timeFormat.format(startHour, startMinute),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Hora Inicial") },
                        trailingIcon = {
                            IconButton(onClick = {
                                TimePickerDialog(context, { _, h, m ->
                                    startHour = h
                                    startMinute = m
                                }, startHour, startMinute, true).show()
                            }) {
                                Icon(Icons.Default.Schedule, contentDescription = "Hora Inicial")
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )

                    // End Time
                    OutlinedTextField(
                        value = timeFormat.format(endHour, endMinute),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Hora Final") },
                        trailingIcon = {
                            IconButton(onClick = {
                                TimePickerDialog(context, { _, h, m ->
                                    endHour = h
                                    endMinute = m
                                }, endHour, endMinute, true).show()
                            }) {
                                Icon(Icons.Default.Schedule, contentDescription = "Hora Final")
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Full Day Checkbox
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = fullDay, onCheckedChange = { fullDay = it })
                Text(text = "Dia inteiro")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (selectedClientName.isEmpty()) {
                        Toast.makeText(context, "Selecione um cliente", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    
                    isSaving = true
                    scope.launch {
                        try {
                            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.000", Locale.getDefault())
                            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                            calendar.timeInMillis = selectedDateMillis
                            
                            val year = calendar.get(Calendar.YEAR)
                            val month = calendar.get(Calendar.MONTH)
                            val day = calendar.get(Calendar.DAY_OF_MONTH)

                            val startCal = Calendar.getInstance().apply {
                                set(year, month, day, if (fullDay) 0 else startHour, if (fullDay) 0 else startMinute, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            val endCal = Calendar.getInstance().apply {
                                set(year, month, day, if (fullDay) 23 else endHour, if (fullDay) 59 else endMinute, 0)
                                set(Calendar.MILLISECOND, 0)
                            }

                            val item = AgendaItem(
                                title = selectedClientName,
                                description = description,
                                startTime = isoFormat.format(startCal.time),
                                endTime = isoFormat.format(endCal.time),
                                fullDay = fullDay
                            )

                            apiService.addAgendaItem(item = item)
                            Toast.makeText(context, "Agendamento realizado!", Toast.LENGTH_SHORT).show()
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
                    CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp), strokeWidth = 2.dp)
                }
                Text("Salvar")
            }

            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Cancelar")
            }
        }

        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { selectedDateMillis = it }
                        showDatePicker = false
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}
