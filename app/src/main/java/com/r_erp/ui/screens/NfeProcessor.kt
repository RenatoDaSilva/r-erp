package com.r_erp.ui.screens

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.r_erp.api.LocalSessionManager
import com.r_erp.api.LocalToken
import com.r_erp.api.SupabaseProduct
import com.r_erp.api.SupabaseService
import kotlinx.coroutines.launch
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import java.util.Locale

@Composable
fun NfeProcessorView(
    data: Map<String, Any>,
    onFinished: (Map<String, Any>) -> Unit,
    onCancel: () -> Unit
) {
    val token = LocalToken.current
    val sessionManager = LocalSessionManager.current
    val supabaseService = remember { SupabaseService.create(token, sessionManager) }
    val scope = rememberCoroutineScope()

    var supplierId by remember { mutableIntStateOf(-1) }
    val items = remember { data["items"] as List<Map<String, Any>> }
    val processedItems = remember { mutableStateListOf<Map<String, Any>>() }
    var currentItemIndex by remember { mutableIntStateOf(0) }
    
    var allProducts by remember { mutableStateOf<List<SupabaseProduct>>(emptyList()) }
    var isProcessing by remember { mutableStateOf(true) }
    var statusMessage by remember { mutableStateOf("Iniciando processamento...") }
    
    var showLinkDialog by remember { mutableStateOf(false) }
    var selectedProductForLinking by remember { mutableStateOf<SupabaseProduct?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        try {
            // Step 2: Supplier handling
            statusMessage = "Identificando fornecedor..."
            val cnpj = data["supplier_cnpj"] as String
            val name = data["supplier_name"] as String
            Log.d("NfeProcessor", "Handling supplier: CNPJ=$cnpj, Name=$name")
            
            var supplier = supabaseService.getSupplierByCnpj("eq.$cnpj").firstOrNull()
            Log.d("NfeProcessor", "Search by CNPJ result: ${supplier?.fullName ?: "Not found"}")
            
            if (supplier == null) {
                supplier = supabaseService.getSupplierByFullname("eq.$name").firstOrNull()
                Log.d("NfeProcessor", "Search by Name result: ${supplier?.fullName ?: "Not found"}")
            }
            
            if (supplier == null) {
                statusMessage = "Cadastrando novo fornecedor..."
                Log.d("NfeProcessor", "Creating new supplier...")
                val resp = supabaseService.createSupplier(
                    supplier = mapOf(
                        "fullname" to name,
                        "cpfcnpj" to cnpj,
                        "address" to (data["supplier_address"] ?: ""),
                        "city" to (data["supplier_city"] ?: ""),
                        "state" to (data["supplier_state"] ?: ""),
                        "phone" to (data["supplier_phone"] ?: "")
                    ).filterValues { it != "" }
                )
                supplierId = resp.body()?.firstOrNull()?.id ?: -1
                Log.d("NfeProcessor", "New supplier created ID: $supplierId")
            } else {
                supplierId = supplier.id ?: -1
                Log.d("NfeProcessor", "Existing supplier found ID: $supplierId")
            }

            if (supplierId == -1) {
                statusMessage = "Erro ao identificar/cadastrar fornecedor."
                Log.e("NfeProcessor", "Failed to get supplier ID")
                isProcessing = false
                return@LaunchedEffect
            }

            // Fetch products for the combobox
            statusMessage = "Carregando produtos..."
            Log.d("NfeProcessor", "Fetching all products...")
            allProducts = supabaseService.getProducts().sortedBy { it.description?.lowercase() ?: "" }
            Log.d("NfeProcessor", "Fetched ${allProducts.size} products")
            
            isProcessing = false
        } catch (e: Exception) {
            Log.e("NfeProcessor", "Error in initial processing", e)
            statusMessage = "Erro: ${e.message}"
            isProcessing = false
        }
    }

    LaunchedEffect(currentItemIndex, supplierId, showLinkDialog, isProcessing) {
        if (supplierId == -1 || showLinkDialog || isProcessing) return@LaunchedEffect
        
        if (currentItemIndex < items.size) {
            val item = items[currentItemIndex]
            val code = (item["product_code"] as? String) ?: ""
            val name = (item["product_name"] as? String) ?: ""
            Log.d("NfeProcessor", "Processing item $currentItemIndex: $name (Code: $code)")

            try {
                Log.d("NfeProcessor", "Searching for mapping: supplierId=$supplierId, code=$code, name=$name")
                val mapping = supabaseService.getProductXSuppliers(
                    supplierId = "eq.$supplierId",
                    code = if (code.isNotEmpty()) "eq.$code" else null,
                    description = if (code.isEmpty()) "eq.$name" else null
                ).firstOrNull()

                if (mapping != null) {
                    Log.d("NfeProcessor", "Mapping found: productId=${mapping.productId}")
                    processedItems.add(item + mapOf("product_id" to mapping.productId!!))
                    currentItemIndex++
                } else {
                    Log.d("NfeProcessor", "No mapping found for item")
                    showLinkDialog = true
                }
            } catch (e: Exception) {
                Log.e("NfeProcessor", "Error checking mapping", e)
                // Maybe show an error message and allow retry? For now just skip to avoid infinite loop
                // but usually mapping check shouldn't fail if DB is up.
            }
        } else {
            Log.d("NfeProcessor", "All items processed successfully")
            onFinished(data + mapOf("items" to processedItems.toList(), "supplier_id" to supplierId))
        }
    }

    if (showLinkDialog && currentItemIndex < items.size) {
        val currentItem = items[currentItemIndex]
        AlertDialog(
            onDismissRequest = { /* Prevent dismiss */ },
            title = { Text("Vincular Produto") },
            text = {
                Column {
                    Text("Fornecedor: ${data["supplier_name"]}", style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Produto (NFC-e): ${currentItem["product_name"]}", style = MaterialTheme.typography.titleMedium)
                    if (((currentItem["product_code"] as? String) ?: "").isNotEmpty()) {
                        Text("Código: ${currentItem["product_code"]}", style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Buscar produto no sistema") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    val filteredProducts = allProducts.filter { 
                        it.description?.contains(searchQuery, ignoreCase = true) == true 
                    }.take(10)
                    
                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                        items(filteredProducts) { product ->
                            ListItem(
                                headlineContent = { Text(product.description ?: "") },
                                supportingContent = { Text("Un: ${product.unit}") },
                                modifier = Modifier.clickable { 
                                    selectedProductForLinking = product
                                    searchQuery = product.description ?: ""
                                },
                                trailingContent = {
                                    if (selectedProductForLinking?.id == product.id) {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                    }
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = selectedProductForLinking != null,
                    onClick = {
                        scope.launch {
                            val product = selectedProductForLinking!!
                            Log.d("NfeProcessor", "Saving manual mapping: supplierId=$supplierId, productId=${product.id}, code=${currentItem["product_code"]}, description=${currentItem["product_name"]}")
                            try {
                                supabaseService.createProductXSupplier(
                                    data = mapOf(
                                        "supplier_id" to supplierId,
                                        "product_id" to product.id!!,
                                        "code" to (currentItem["product_code"] ?: ""),
                                        "description" to (currentItem["product_name"] ?: "")
                                    )
                                )
                                Log.d("NfeProcessor", "Manual mapping saved")
                                processedItems.add(currentItem + mapOf("product_id" to product.id))
                                showLinkDialog = false
                                selectedProductForLinking = null
                                searchQuery = ""
                                currentItemIndex++
                            } catch (e: Exception) {
                                Log.e("NfeProcessor", "Error saving manual mapping", e)
                            }
                        }
                    }
                ) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        scope.launch {
                            Log.d("NfeProcessor", "Creating new product and mapping for item: ${currentItem["product_name"]}")
                            try {
                                val resp = supabaseService.createProduct(
                                    product = mapOf(
                                        "description" to (currentItem["product_name"] ?: ""),
                                        "unit" to (currentItem["product_unit"] ?: ""),
                                        "generates_stock" to true,
                                        "price" to 0.0,
                                        "cost" to (currentItem["price"] ?: 0.0)
                                    )
                                )
                                val newProduct = resp.body()?.firstOrNull()
                                if (newProduct != null) {
                                    Log.d("NfeProcessor", "New product created ID: ${newProduct.id}")
                                    supabaseService.createProductXSupplier(
                                        data = mapOf(
                                            "supplier_id" to supplierId,
                                            "product_id" to newProduct.id!!,
                                            "code" to (currentItem["product_code"] ?: ""),
                                            "description" to (currentItem["product_name"] ?: "")
                                        )
                                    )
                                    Log.d("NfeProcessor", "Mapping for new product saved")
                                    processedItems.add(currentItem + mapOf("product_id" to newProduct.id))
                                    
                                    // Refresh products list
                                    allProducts = supabaseService.getProducts().sortedBy { it.description?.lowercase() ?: "" }

                                    showLinkDialog = false
                                    selectedProductForLinking = null
                                    searchQuery = ""
                                    currentItemIndex++
                                } else {
                                    Log.e("NfeProcessor", "Failed to get new product representation")
                                }
                            } catch (e: Exception) {
                                Log.e("NfeProcessor", "Error creating product and mapping", e)
                            }
                        }
                    }) {
                        Text("Criar novo")
                    }
                    TextButton(onClick = onCancel) {
                        Text("Cancelar")
                    }
                }
            }
        )
    }

    if (isProcessing || (currentItemIndex >= items.size && !showLinkDialog && supplierId != -1)) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(if (isProcessing) statusMessage else "Finalizando processamento...")
            }
        }
    }
}

fun parseNfeXml(xml: String): Map<String, Any>? {
    try {
        val doc = Jsoup.parse(xml, "", Parser.xmlParser())
        
        // nfeProc > NFe > infNFe > ide > finNFe
        val finNFe = doc.select("ide > finNFe").text().ifEmpty {
            doc.select("infNFe > ide > finNFe").text()
        }
        
        if (finNFe.isNotEmpty() && finNFe != "1") {
            return mapOf("error" to "Nota fiscal com finalidade diferente de venda normal")
        }

        // ide > dhEmi
        val dhEmi = doc.select("ide > dhEmi").text()
        
        // emit > CNPJ
        val supplierCnpj = doc.select("emit > CNPJ").text()
        
        // emit > xNome
        val supplierName = doc.select("emit > xNome").text()
        
        // emit > enderEmit
        val enderEmit = doc.select("emit > enderEmit")
        val xLgr = enderEmit.select("xLgr").text()
        val nro = enderEmit.select("nro").text()
        val xBairro = enderEmit.select("xBairro").text()
        val supplierAddress = listOf(xLgr, nro, xBairro).filter { it.isNotBlank() }.joinToString(", ")
        
        val supplierCity = enderEmit.select("xMun").text()
        val supplierState = enderEmit.select("UF").text()
        val supplierPhone = enderEmit.select("fone").text()

        val items = mutableListOf<Map<String, Any>>()
        var totalTax = 0.0
        
        doc.select("det").forEach { det ->
            val prod = det.select("prod")
            val productCode = prod.select("cProd").text()
            val productName = prod.select("xProd").text()
            val productUnit = prod.select("uCom").text()
            val qty = prod.select("qCom").text().toDoubleOrNull() ?: 0.0
            val price = prod.select("vUnCom").text().toDoubleOrNull() ?: 0.0
            val discount = prod.select("vDesc").text().toDoubleOrNull() ?: 0.0
            
            // Imposto may have vTotTrib inside or other tags. We'll try to get it.
            val itemTax = det.select("imposto vTotTrib").text().toDoubleOrNull() ?: 
                         det.select("imposto").text().toDoubleOrNull() ?: 0.0
            totalTax += itemTax

            if (productName.isNotEmpty()) {
                items.add(mapOf(
                    "product_name" to productName,
                    "product_code" to productCode,
                    "product_unit" to productUnit,
                    "quantity" to qty,
                    "price" to price,
                    "discount" to discount
                ))
            }
        }

        if (supplierName.isEmpty() && items.isEmpty()) return null

        return mapOf(
            "supplier_name" to supplierName,
            "supplier_cnpj" to supplierCnpj,
            "supplier_address" to supplierAddress,
            "supplier_city" to supplierCity,
            "supplier_state" to supplierState,
            "supplier_phone" to supplierPhone,
            "items" to items,
            "created_at" to dhEmi,
            "tax" to totalTax
        )
    } catch (e: Exception) {
        return null
    }
}

fun parseNfceHtml(html: String): Map<String, Any>? {
    Log.d("NfeParser", "Starting HTML parsing...")
    val doc = Jsoup.parse(html)
    
    val supplierName = doc.select(".txtTopo").first()?.text() 
        ?: doc.select("#conteudo .txtCenter").first()?.text() ?: ""
    Log.d("NfeParser", "Supplier Name: $supplierName")
    
    // Attempt to extract the entire info block to search for CNPJ and Address
    val infoBlock = doc.select("#conteudo .text").text().ifEmpty { 
        doc.select("#conteudo").text() 
    }
    
    val supplierCnpj = Regex("""CNPJ:\s*([\d./-]+)""").find(infoBlock)?.groupValues?.get(1) ?: ""
    Log.d("NfeParser", "Supplier CNPJ: $supplierCnpj")

    val supplierAddress = if (infoBlock.contains("Endereço:")) {
        infoBlock.substringAfter("Endereço:").substringBefore("CNPJ:").trim()
    } else ""
    Log.d("NfeParser", "Supplier Address: $supplierAddress")

    val items = mutableListOf<Map<String, Any>>()
    val rows = doc.select("tr[id^=Item +]")
    Log.d("NfeParser", "Found ${rows.size} item rows (standard)")
    
    rows.forEach { row ->
        val nameRaw = row.select(".txtTit").text()
        
        // Extract code if present: (Código: 120127 )
        val codeMatch = Regex("""\(Código:\s*(.+?)\s*\)""").find(nameRaw)
        val productCode = codeMatch?.groupValues?.get(1)?.trim() ?: ""
        
        // Remove "Vl. Total..." and "(Código...)" from the name
        val name = nameRaw.substringBefore("(Código").substringBefore("Vl. Total").trim()
        
        // Try multiple selectors for details
        var details = row.select(".R_Text").text()
        if (details.isEmpty()) {
            details = row.select("td").joinToString(" ") { it.text() }
        }
        
        val qty = Regex("""Qtde\.:\s*([\d,.]+)""").find(details)?.groupValues?.get(1)?.replace(".", "")?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
        val unit = Regex("""UN:\s*(\w+)""").find(details)?.groupValues?.get(1) ?: ""
        val price = Regex("""Vl\. Unit\.:\s*([\d,.]+)""").find(details)?.groupValues?.get(1)?.replace(".", "")?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
        
        if (name.isNotEmpty()) {
            items.add(mapOf(
                "product_name" to name,
                "product_code" to productCode,
                "product_unit" to unit,
                "quantity" to qty,
                "price" to price
            ))
        }
    }

    if (supplierName.isEmpty() || items.isEmpty()) {
        Log.d("NfeParser", "Standard parsing failed, trying fallback...")
        val table = doc.select("table#tabResult").first()
        if (table != null) {
            val rowsFallback = table.select("tr")
            Log.d("NfeParser", "Found ${rowsFallback.size} fallback rows")
            rowsFallback.forEach { r ->
                val nameRaw = r.select("td.txtTit").text()
                if (nameRaw.isNotEmpty()) {
                    val codeMatch = Regex("""\(Código:\s*(.+?)\s*\)""").find(nameRaw)
                    val productCode = codeMatch?.groupValues?.get(1)?.trim() ?: ""
                    val name = nameRaw.substringBefore("(Código").substringBefore("Vl. Total").trim()
                    
                    var details = r.select("td.R_Text").text()
                    if (details.isEmpty()) {
                        details = r.select("td").joinToString(" ") { it.text() }
                    }
                    
                    val qty = Regex("""Qtde\.:\s*([\d,.]+)""").find(details)?.groupValues?.get(1)?.replace(".", "")?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
                    val unit = Regex("""UN:\s*(\w+)""").find(details)?.groupValues?.get(1) ?: ""
                    val price = Regex("""Vl\. Unit\.:\s*([\d,.]+)""").find(details)?.groupValues?.get(1)?.replace(".", "")?.replace(",", ".")?.toDoubleOrNull() ?: 0.0

                    items.add(mapOf(
                        "product_name" to name,
                        "product_code" to productCode,
                        "product_unit" to unit,
                        "quantity" to qty,
                        "price" to price
                    ))
                }
            }
        }
    }

    Log.d("NfeParser", "Parsing finished. Found ${items.size} items.")
    if (supplierName.isEmpty() && items.isEmpty()) {
        return null
    }

    return mapOf(
        "supplier_name" to supplierName,
        "supplier_cnpj" to supplierCnpj,
        "supplier_address" to supplierAddress,
        "items" to items
    )
}
