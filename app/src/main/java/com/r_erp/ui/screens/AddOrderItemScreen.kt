package com.r_erp.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.r_erp.api.SupabaseService
import com.r_erp.api.SupabaseProduct
import com.r_erp.api.SupabaseServiceItem
import com.r_erp.api.SupabaseOrderItem
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOrderItemScreen(
    onItemAdded: (SupabaseOrderItem) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val supabaseService = remember { SupabaseService.create() }

    var isLoading by remember { mutableStateOf(true) }
    var isService by remember { mutableStateOf(false) }
    
    // Data lists
    var services by remember { mutableStateOf<List<SupabaseServiceItem>>(emptyList()) }
    var products by remember { mutableStateOf<List<SupabaseProduct>>(emptyList()) }
    
    // Selection state
    var selectedService by remember { mutableStateOf<SupabaseServiceItem?>(null) }
    var selectedProduct by remember { mutableStateOf<SupabaseProduct?>(null) }
    var itemExpanded by remember { mutableStateOf(false) }
    
    // Input fields
    var quantity by remember { mutableStateOf("1.00") }
    var price by remember { mutableStateOf("0.00") }
    var discount by remember { mutableStateOf("0.00") }

    LaunchedEffect(Unit) {
        try {
            services = supabaseService.getServices()
            products = supabaseService.getProducts()
            isLoading = false
        } catch (e: Exception) {
            Toast.makeText(context, "Erro ao carregar dados: ${e.message}", Toast.LENGTH_LONG).show()
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = "Adicionar Item", style = MaterialTheme.typography.headlineSmall)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isService,
                        onCheckedChange = { 
                            isService = it 
                            selectedService = null
                            selectedProduct = null
                            price = "0.00"
                        }
                    )
                    Text(text = "É serviço")
                }

                // Service or Product Dropdown
                ExposedDropdownMenuBox(
                    expanded = itemExpanded,
                    onExpandedChange = { itemExpanded = !itemExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = if (isService) selectedService?.description ?: "" else selectedProduct?.description ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(if (isService) "Serviço" else "Produto") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = itemExpanded) },
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = itemExpanded,
                        onDismissRequest = { itemExpanded = false }
                    ) {
                        if (isService) {
                            services.forEach { service ->
                                DropdownMenuItem(
                                    text = { Text(service.description ?: "") },
                                    onClick = {
                                        selectedService = service
                                        price = String.format(Locale.US, "%.2f", service.price ?: 0.0)
                                        itemExpanded = false
                                    }
                                )
                            }
                        } else {
                            products.forEach { product ->
                                DropdownMenuItem(
                                    text = { Text(product.description ?: "") },
                                    onClick = {
                                        selectedProduct = product
                                        price = String.format(Locale.US, "%.2f", product.price ?: 0.0)
                                        itemExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

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

                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Preço") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = discount,
                    onValueChange = { discount = it },
                    label = { Text("Desconto") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val q = quantity.toDoubleOrNull() ?: 0.0
                            val p = price.toDoubleOrNull() ?: 0.0
                            val d = discount.toDoubleOrNull() ?: 0.0
                            val total = (q * p) - d
                            
                            val newItem = SupabaseOrderItem(
                                productId = if (!isService) selectedProduct?.id else null,
                                serviceId = if (isService) selectedService?.id else null,
                                description = if (isService) selectedService?.description else selectedProduct?.description,
                                quantity = q,
                                price = p,
                                discount = d,
                                total = total
                            )
                            onItemAdded(newItem)
                        },
                        modifier = Modifier.weight(1f),
                        enabled = (isService && selectedService != null) || (!isService && selectedProduct != null)
                    ) {
                        Text("Adicionar")
                    }
                    TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                        Text("Cancelar")
                    }
                }
            }
        }
    }
}
