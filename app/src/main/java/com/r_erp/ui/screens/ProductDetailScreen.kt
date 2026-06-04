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
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import com.r_erp.api.LocalToken
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(productId: Int, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val token = LocalToken.current
    val supabaseService = remember(token) { SupabaseService.create(token) }
    var isLoading by remember { mutableStateOf(value = true) }
    var isSaving by remember { mutableStateOf(value = false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Editable states
    var description by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }
    var priceStr by remember { mutableStateOf("") }
    var stockStr by remember { mutableStateOf("") }
    var costStr by remember { mutableStateOf("") }
    var generatesStock by remember { mutableStateOf(true) }

    // Lists for ComboBoxes
    var typeOptions by remember { mutableStateOf<List<String>>(emptyList()) }
    var unitOptions by remember { mutableStateOf<List<String>>(emptyList()) }
    var typeExpanded by remember { mutableStateOf(false) }
    var unitExpanded by remember { mutableStateOf(false) }

    val localeBR = Locale("pt", "BR")
    val currencyFormatter = NumberFormat.getCurrencyInstance(localeBR)

    LaunchedEffect(productId, supabaseService) {
        try {
            // Load types from Supabase
            val supabaseTypes = supabaseService.getProductTypes()
            typeOptions = supabaseTypes.mapNotNull { it.type }
            
            // Load units from Supabase
            val supabaseUnits = supabaseService.getProductUnits()
            unitOptions = supabaseUnits.mapNotNull { it.unit }

            if (productId != -1) {
                val fetchedProducts = supabaseService.getProduct(idFilter = "eq.$productId")
                if (fetchedProducts.isNotEmpty()) {
                    val fetchedProduct = fetchedProducts[0]
                    description = fetchedProduct.description ?: ""
                    type = fetchedProduct.type ?: ""
                    unit = fetchedProduct.unit ?: ""
                    priceStr = String.format(Locale.US, "%.2f", fetchedProduct.price ?: 0.0)
                    stockStr = String.format(Locale.US, "%.2f", fetchedProduct.stock ?: 0.0)
                    costStr = String.format(Locale.US, "%.2f", fetchedProduct.cost ?: 0.0)
                    generatesStock = fetchedProduct.generatesStock ?: true
                } else {
                    errorMessage = "Produto não encontrado"
                }
            } else {
                priceStr = "0.00"
                stockStr = "0.00"
                costStr = "0.00"
            }
            isLoading = false
        } catch (e: Exception) {
            if (e.message?.contains("composition") != true) {
                errorMessage = e.message ?: "Erro ao carregar dados"
            }
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
                if (productId == -1) {
                    Text(text = "Novo Produto", style = MaterialTheme.typography.headlineSmall)
                } else {
                    Text(text = "Editar Produto ID: $productId", style = MaterialTheme.typography.headlineSmall)
                }

                // Read-only ID if editing
                if (productId != -1) {
                    OutlinedTextField(
                        value = productId.toString(),
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

                // Type ComboBox
                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = !typeExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = type,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        typeOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    type = option
                                    typeExpanded = false
                                }
                            )
                        }
                    }
                }

                // Unit ComboBox
                ExposedDropdownMenuBox(
                    expanded = unitExpanded,
                    onExpandedChange = { unitExpanded = !unitExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = unit,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Unidade") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = unitExpanded,
                        onDismissRequest = { unitExpanded = false }
                    ) {
                        unitOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    unit = option
                                    unitExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = priceStr,
                    onValueChange = { priceStr = it },
                    label = { Text("Preço") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = costStr,
                    onValueChange = { costStr = it },
                    label = { Text("Custo") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = stockStr,
                    onValueChange = { stockStr = it },
                    label = { Text("Estoque") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = generatesStock,
                        onCheckedChange = { generatesStock = it }
                    )
                    Text(text = "Controla estoque")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        isSaving = true
                        scope.launch {
                            try {
                                val productMap = mutableMapOf<String, Any>()
                                if (description.isNotBlank()) productMap["description"] = description
                                if (type.isNotBlank()) productMap["type"] = type
                                if (unit.isNotBlank()) productMap["unit"] = unit
                                
                                val p = priceStr.toDoubleOrNull() ?: 0.0
                                productMap["price"] = p
                                
                                val s = stockStr.toDoubleOrNull() ?: 0.0
                                productMap["stock"] = s
                                
                                val c = costStr.toDoubleOrNull() ?: 0.0
                                productMap["cost"] = c

                                productMap["generates_stock"] = generatesStock
                                
                                if (productId == -1) {
                                    val response = supabaseService.createProduct(product = productMap)
                                    if (!response.isSuccessful) throw retrofit2.HttpException(response)
                                } else {
                                    val response = supabaseService.updateProduct(
                                        idFilter = "eq.$productId",
                                        product = productMap
                                    )
                                    if (!response.isSuccessful) throw retrofit2.HttpException(response)
                                }

                                Toast.makeText(context, "Produto salvo com sucesso!", Toast.LENGTH_SHORT).show()
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
