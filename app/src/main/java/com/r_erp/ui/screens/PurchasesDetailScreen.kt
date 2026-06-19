package com.r_erp.ui.screens

import android.app.DatePickerDialog
import android.util.Log
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.r_erp.api.SupabaseSupplier
import com.r_erp.api.SupabaseService
import com.r_erp.api.LocalToken
import com.r_erp.api.LocalSessionManager
import com.r_erp.api.SupabasePurchaseItem
import com.r_erp.api.SupabasePurchaseItemRequest
import com.r_erp.api.SupabaseProduct
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchasesDetailScreen(purchaseId: Int? = null, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val token = LocalToken.current
    val sessionManager = LocalSessionManager.current
    val supabaseService = remember(token) { SupabaseService.create(token, sessionManager) }

    var isLoading by remember { mutableStateOf(true) }
    var nextId by remember { mutableStateOf<Int?>(purchaseId) }
    
    // Purchase Header Data
    var selectedSupplier by remember { mutableStateOf<SupabaseSupplier?>(null) }
    var payUntil by remember { mutableStateOf("") }
    var discount by remember { mutableStateOf("0.00") }
    var freight by remember { mutableStateOf("0.00") }
    var tax by remember { mutableStateOf("0.00") }
    var message by remember { mutableStateOf("") }
    
    // Items list (in memory)
    val purchaseItems = remember { mutableStateListOf<SupabasePurchaseItem>() }
    
    // Options lists
    var suppliers by remember { mutableStateOf<List<SupabaseSupplier>>(emptyList()) }
    var allProducts by remember { mutableStateOf<List<SupabaseProduct>>(emptyList()) }
    var supplierExpanded by remember { mutableStateOf(false) }
    
    // Sub-screen state
    var isAddingItem by remember { mutableStateOf(false) }
    var errorDialogMessage by remember { mutableStateOf<String?>(null) }

    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val displayDateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.US) }

    LaunchedEffect(supabaseService) {
        try {
            suppliers = supabaseService.getSuppliers().sortedBy { it.fullName?.lowercase() ?: "" }
            allProducts = supabaseService.getProducts().sortedBy { it.description?.lowercase() ?: "" }

            if (purchaseId == null) {
                try {
                    nextId = supabaseService.getSequence(mapOf("sequence_name" to "purchases_id_seq"))
                } catch (e: Exception) {
                    // Fallback if sequence is not found or fails
                    Log.w("PurchasesDetail", "Could not fetch sequence: ${e.message}")
                    nextId = null 
                }
            } else {
                val fetchedPurchases = supabaseService.getPurchaseWithItems(idFilter = "eq.$purchaseId")
                if (fetchedPurchases.isNotEmpty()) {
                    val p = fetchedPurchases[0]
                    selectedSupplier = suppliers.find { it.id == p.supplierId }
                    payUntil = p.payUntil?.take(10) ?: ""
                    discount = String.format(Locale.US, "%.2f", p.discount ?: 0.0)
                    freight = String.format(Locale.US, "%.2f", p.freight ?: 0.0)
                    tax = String.format(Locale.US, "%.2f", p.tax ?: 0.0)
                    message = p.message ?: ""
                    
                    purchaseItems.clear()
                    val itemsFromTable = supabaseService.getPurchaseItems(purchaseIdFilter = "eq.$purchaseId")
                    if (itemsFromTable.isNotEmpty()) {
                        purchaseItems.addAll(itemsFromTable)
                    } else if (p.items != null) {
                        purchaseItems.addAll(p.items)
                    }
                } else {
                    Toast.makeText(context, "Compra não encontrada", Toast.LENGTH_SHORT).show()
                    onBack()
                }
            }
            isLoading = false
        } catch (e: Exception) {
            Toast.makeText(context, "Erro ao carregar dados: ${e.message}", Toast.LENGTH_LONG).show()
            isLoading = false
        }
    }

    if (isAddingItem) {
        AddPurchaseItemScreen(
            onItemAdded = { newItem ->
                purchaseItems.add(newItem)
                isAddingItem = false
            },
            onCancel = { isAddingItem = false }
        )
        return
    }

    Scaffold { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val titleLabel = if (purchaseId == null) "Nova Compra" else "Editar Compra"
                Text(text = "$titleLabel ID: ${nextId ?: "..."}", style = MaterialTheme.typography.headlineSmall)

                // Supplier Selection
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
                    ExposedDropdownMenu(
                        expanded = supplierExpanded,
                        onDismissRequest = { supplierExpanded = false }
                    ) {
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

                // Pay Until Date
                OutlinedTextField(
                    value = if (payUntil.isNotEmpty()) {
                        try { displayDateFormatter.format(dateFormatter.parse(payUntil)!!) } catch (e: Exception) { payUntil }
                    } else "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Pagar até") },
                    trailingIcon = {
                        IconButton(onClick = {
                            val calendar = Calendar.getInstance()
                            if (payUntil.isNotEmpty()) {
                                try { calendar.time = dateFormatter.parse(payUntil)!! } catch (e: Exception) {}
                            }
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val cal = Calendar.getInstance()
                                    cal.set(year, month, dayOfMonth)
                                    payUntil = dateFormatter.format(cal.time)
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Selecionar data")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = discount,
                        onValueChange = { discount = it },
                        label = { Text("Desconto") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = freight,
                        onValueChange = { freight = it },
                        label = { Text("Frete") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                OutlinedTextField(
                    value = tax,
                    onValueChange = { tax = it },
                    label = { Text("Impostos/Outras Taxas") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Observações") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Itens da Compra", style = MaterialTheme.typography.titleMedium)
                    Button(onClick = { isAddingItem = true }) {
                        Text("Novo Item")
                    }
                }

                // Items list
                purchaseItems.forEachIndexed { index, item ->
                    val displayDescription = item.description 
                        ?: item.product?.description
                        ?: allProducts.find { it.id == item.productId }?.description 
                        ?: "Produto desconhecido"
                    
                    val itemTotal = (item.quantity ?: 0.0) * (item.price ?: 0.0) - (item.discount ?: 0.0)

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(text = displayDescription, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    Text(text = "x${String.format(Locale.US, "%.2f", item.quantity ?: 0.0)}")
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(text = "Preço: ${String.format(Locale.US, "%.2f", item.price ?: 0.0)}", style = MaterialTheme.typography.bodySmall)
                                    Text(text = "Total: ${String.format(Locale.US, "%.2f", itemTotal)}", fontWeight = FontWeight.Bold)
                                }
                            }
                            IconButton(onClick = { purchaseItems.removeAt(index) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remover item", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        if (index < purchaseItems.size - 1) HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Totals
                val subTotal = purchaseItems.sumOf { (it.quantity ?: 0.0) * (it.price ?: 0.0) - (it.discount ?: 0.0) }
                val d = discount.toDoubleOrNull() ?: 0.0
                val f = freight.toDoubleOrNull() ?: 0.0
                val t = tax.toDoubleOrNull() ?: 0.0
                val total = subTotal - d + f + t

                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                    Text(text = "Sub-total: ${String.format(Locale.US, "%.2f", subTotal)}", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "TOTAL: ${String.format(Locale.US, "%.2f", total)}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { 
                            if (selectedSupplier == null) {
                                Toast.makeText(context, "Selecione um fornecedor", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (purchaseItems.isEmpty()) {
                                Toast.makeText(context, "Adicione pelo menos um item", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            scope.launch {
                                try {
                                    val purchaseMap = mutableMapOf<String, Any>()
                                    purchaseMap["id"] = nextId!!
                                    purchaseMap["supplier_id"] = selectedSupplier!!.id!!
                                    if (payUntil.isNotEmpty()) purchaseMap["pay_until"] = payUntil
                                    purchaseMap["discount"] = d
                                    purchaseMap["freight"] = f
                                    purchaseMap["tax"] = t
                                    if (message.isNotBlank()) purchaseMap["message"] = message

                                    if (purchaseId == null) {
                                        val response = supabaseService.createPurchase(purchaseMap)
                                        if (!response.isSuccessful) throw Exception("Erro ao criar compra: ${response.errorBody()?.string()}")
                                    } else {
                                        val response = supabaseService.updatePurchase(idFilter = "eq.$purchaseId", purchase = purchaseMap)
                                        if (!response.isSuccessful) throw Exception("Erro ao atualizar compra: ${response.errorBody()?.string()}")
                                        
                                        supabaseService.deletePurchaseItems(purchaseIdFilter = "eq.$purchaseId")
                                    }

                                    val itemsRequest = purchaseItems.map { 
                                        SupabasePurchaseItemRequest(
                                            purchaseId = nextId,
                                            productId = it.productId,
                                            quantity = it.quantity,
                                            price = it.price,
                                            discount = it.discount
                                        )
                                    }
                                    if (itemsRequest.isNotEmpty()) {
                                        val itemsResponse = supabaseService.createPurchaseItems(itemsRequest)
                                        if (!itemsResponse.isSuccessful) throw Exception("Erro ao salvar itens: ${itemsResponse.errorBody()?.string()}")
                                    }

                                    Toast.makeText(context, "Compra salva com sucesso!", Toast.LENGTH_SHORT).show()
                                    onBack()
                                } catch (e: Exception) {
                                    Log.e("PurchasesDetail", "Error saving purchase", e)
                                    errorDialogMessage = e.message ?: "Erro desconhecido"
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Salvar")
                    }
                    TextButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                        Text("Cancelar")
                    }
                }
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
}
