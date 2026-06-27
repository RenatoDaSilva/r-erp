package com.r_erp.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.r_erp.api.LocalSessionManager
import com.r_erp.api.LocalToken
import com.r_erp.api.SupabaseProduct
import com.r_erp.api.SupabaseService
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToBuyDetailScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val token = LocalToken.current
    val sessionManager = LocalSessionManager.current
    val supabaseService = remember(token) { SupabaseService.create(token, sessionManager) }

    var isLoading by remember { mutableStateOf(true) }
    var products by remember { mutableStateOf<List<SupabaseProduct>>(emptyList()) }
    var selectedProduct by remember { mutableStateOf<SupabaseProduct?>(null) }
    var productExpanded by remember { mutableStateOf(false) }
    
    var quantity by remember { mutableStateOf("1.00") }

    LaunchedEffect(supabaseService) {
        try {
            products = supabaseService.getProducts().sortedBy { it.description?.lowercase() ?: "" }
            isLoading = false
        } catch (e: Exception) {
            Toast.makeText(context, "Erro ao carregar produtos: ${e.message}", Toast.LENGTH_LONG).show()
            isLoading = false
        }
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(text = "Adicionar Item para Comprar", style = MaterialTheme.typography.headlineSmall)

                // Product ComboBox
                ExposedDropdownMenuBox(
                    expanded = productExpanded,
                    onExpandedChange = { productExpanded = !productExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedProduct?.description ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Produto") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = productExpanded) },
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = productExpanded,
                        onDismissRequest = { productExpanded = false }
                    ) {
                        products.forEach { product ->
                            DropdownMenuItem(
                                text = { Text(product.description ?: "") },
                                onClick = {
                                    selectedProduct = product
                                    productExpanded = false
                                }
                            )
                        }
                    }
                }

                // Quantity with +/- buttons
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantidade") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        IconButton(onClick = {
                            val current = quantity.toDoubleOrNull() ?: 1.0
                            if (current > 1.0) {
                                quantity = String.format(Locale.US, "%.2f", current - 1.0)
                            } else {
                                quantity = "1.00"
                            }
                        }) {
                            Icon(Icons.Default.Remove, contentDescription = "Diminuir")
                        }
                    },
                    trailingIcon = {
                        IconButton(onClick = {
                            val current = quantity.toDoubleOrNull() ?: 1.0
                            quantity = String.format(Locale.US, "%.2f", current + 1.0)
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "Aumentar")
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (selectedProduct == null) {
                                Toast.makeText(context, "Selecione um produto", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            
                            scope.launch {
                                try {
                                    val data = mapOf(
                                        "product_id" to selectedProduct!!.id!!,
                                        "quantity" to (quantity.toDoubleOrNull() ?: 1.0),
                                        "log" to listOf("Inserção manual")
                                    )
                                    val response = supabaseService.createToBuy(data)
                                    if (response.isSuccessful) {
                                        Toast.makeText(context, "Salvo com sucesso!", Toast.LENGTH_SHORT).show()
                                        onBack()
                                    } else {
                                        Toast.makeText(context, "Erro: ${response.errorBody()?.string()}", Toast.LENGTH_LONG).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("OK")
                    }
                    TextButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar")
                    }
                }
            }
        }
    }
}
