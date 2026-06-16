package com.r_erp.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.r_erp.api.LocalSessionManager
import com.r_erp.api.LocalToken
import com.r_erp.api.SupabaseProduct
import com.r_erp.api.SupabaseService
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import org.jsoup.Jsoup
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun NfceScannerScreen(
    onNfceDataExtracted: (Map<String, Any>) -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    var scannedUrl by remember { mutableStateOf<String?>(null) }
    var extractedData by remember { mutableStateOf<Map<String, Any>?>(null) }

    if (extractedData != null) {
        NfceProcessorView(
            data = extractedData!!,
            onFinished = { processedData ->
                onNfceDataExtracted(processedData)
            },
            onCancel = { 
                extractedData = null
                scannedUrl = null
            }
        )
    } else if (scannedUrl != null) {
        NfceLoaderView(
            url = scannedUrl!!,
            onDataExtracted = { extractedData = it },
            onCancel = { scannedUrl = null }
        )
    } else {
        if (hasCameraPermission) {
            CameraScannerView(
                onUrlScanned = { url ->
                    scannedUrl = url
                },
                onCancel = onCancel
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Permissão de câmera necessária para ler o QR Code.")
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    launcher.launch(Manifest.permission.CAMERA)
                }) {
                    Text("Pedir Permissão")
                }
                TextButton(onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }) {
                    Text("Abrir Configurações")
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onCancel) {
                    Text("Cancelar")
                }
            }
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
fun CameraScannerView(onUrlScanned: (String) -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    
    val previewView = remember { PreviewView(context) }
    
    LaunchedEffect(previewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            val scanner = BarcodeScanning.getClient(
                BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                    .build()
            )

            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                processImageProxy(scanner, imageProxy, onUrlScanned)
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                Log.e("CameraScanner", "Use case binding failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        
        IconButton(
            onClick = onCancel,
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
        ) {
            Text("Fechar", color = MaterialTheme.colorScheme.onPrimary)
        }
        
        Text(
            "Aponte para o QR Code da Nota Fiscal",
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 64.dp),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@ExperimentalGetImage
private fun processImageProxy(
    scanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    imageProxy: ImageProxy,
    onUrlScanned: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    barcode.rawValue?.let { url ->
                        if (url.startsWith("http")) {
                            onUrlScanned(url)
                        }
                    }
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun NfceLoaderView(
    url: String,
    onDataExtracted: (Map<String, Any>) -> Unit,
    onCancel: () -> Unit,
) {
    var statusMessage by remember { mutableStateOf("Conectando ao portal SEF/SC...") }
    var isLoading by remember { mutableStateOf(true) }
    var showWebView by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val webView = remember {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.userAgentString = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36"
            
            webChromeClient = android.webkit.WebChromeClient()
            webViewClient = object : WebViewClient() {
                private var checkCount = 0
                
                override fun onPageFinished(view: WebView?, currentUrl: String?) {
                    super.onPageFinished(view, currentUrl)
                    Log.d("NfceLoader", "onPageFinished: $currentUrl")
                    
                    val handler = android.os.Handler(android.os.Looper.getMainLooper())
                    val checkContent = object : Runnable {
                        override fun run() {
                            if (!isLoading) return
                            checkCount++
                            
                            if (checkCount > 15 && !showWebView) {
                                showWebView = true
                                statusMessage = "Por favor, complete a verificação na tela abaixo."
                            }

                            view?.evaluateJavascript(
                                """
                                (function() {
                                    var isChallenge = document.title.includes("Just a moment") || 
                                                      document.querySelector('#challenge-running') || 
                                                      document.querySelector('.cf-browser-verification') ||
                                                      document.querySelector('#turnstile-wrapper') ||
                                                      document.body.innerText.includes("Verificando se a conexão") ||
                                                      document.body.innerText.includes("Confirme que é humano");
                                    
                                    if (isChallenge) {
                                        var bodyText = document.body.innerText;
                                        if (bodyText.includes("Confirme que é humano") && !bodyText.includes("Sucesso!")) {
                                            var checkbox = document.querySelector('input[type="checkbox"]') || 
                                                           document.querySelector('.ctp-checkbox-container input') ||
                                                           document.querySelector('#challenge-stage input');
                                            if (checkbox && !checkbox.checked) {
                                                checkbox.click();
                                                return { status: "waiting_verification", subStatus: "clicking" };
                                            }
                                        }
                                        if (bodyText.includes("Sucesso!")) {
                                            return { status: "waiting_verification", subStatus: "success" };
                                        }
                                        return { status: "waiting_verification" };
                                    }

                                    var bodyText = document.body.innerText;
                                    var hasData = !!document.querySelector('.txtTit') || 
                                                  !!document.querySelector('.txtTopo') ||
                                                  bodyText.includes("DOCUMENTO AUXILIAR DA NOTA FISCAL DE CONSUMIDOR ELETRÔNICA") ||
                                                  bodyText.includes("Chave de Acesso");
                                    
                                    if (hasData) {
                                        return { status: "ready", html: document.documentElement.outerHTML };
                                    }

                                    if (bodyText.includes("Verificando...")) {
                                        return { status: "waiting_verification" };
                                    }

                                    var btn = document.getElementById('Body_Main_ButtonValidar') || 
                                              document.getElementById('Body_Main_ButtonVisualizar') ||
                                              document.getElementById('visualizar') || 
                                              document.querySelector('.sat-btn-text')?.parentElement ||
                                              document.querySelector('input[value*="Visualizar"]') ||
                                              document.querySelector('input[value*="Validar"]') ||
                                              document.querySelector('button') || 
                                              document.querySelector('input[type="button"]');
                                    
                                    if (!btn || btn.disabled) {
                                        var allBtns = document.querySelectorAll('button, .sat-btn-text, a, input[type="button"], input[type="submit"]');
                                        for (var i = 0; i < allBtns.length; i++) {
                                            var text = (allBtns[i].value || allBtns[i].innerText || "").toUpperCase();
                                            if (text.includes("VALIDAR") || text.includes("VISUALIZAR")) {
                                                btn = allBtns[i];
                                                break;
                                            }
                                        }
                                    }

                                    if (btn && !btn.disabled) {
                                        if (window._nfceShowed) return { status: "waiting_user" };

                                        var clickKey = "nfce_clicked_" + window.location.href;
                                        if (sessionStorage.getItem(clickKey)) {
                                            return { status: "clicked", waiting: true };
                                        }
                                        
                                        sessionStorage.setItem(clickKey, "true");
                                        btn.click();
                                        if (btn.type === "submit" && btn.form) {
                                            btn.form.submit();
                                        }
                                        return { status: "clicked" };
                                    }
                                    
                                    return { status: "waiting", reason: "no_button_found" };
                                })()
                                """.trimIndent()
                            ) { resultJson ->
                                try {
                                    val result = com.google.gson.JsonParser.parseString(resultJson ?: "{}").asJsonObject
                                    val status = result.get("status").asString
                                    
                                    when (status) {
                                        "ready" -> {
                                            statusMessage = "Dados encontrados! Processando..."
                                            val html = result.get("html").asString
                                            val unescapedHtml = html.replace("\\u003C", "<")
                                                .replace("\\\"", "\"")
                                                .replace("\\n", "\n")
                                            
                                            val data = parseNfceHtml(unescapedHtml)
                                            if (data != null) {
                                                isLoading = false
                                                onDataExtracted(data)
                                            } else {
                                                statusMessage = "Erro ao interpretar dados da nota."
                                            }
                                        }
                                        "clicked" -> {
                                            val isWaiting = result.get("waiting")?.asBoolean ?: false
                                            statusMessage = if (isWaiting) "Aguardando resposta do portal..." else "Acessando nota fiscal..."
                                            handler.postDelayed(this, 3000)
                                        }
                                        "waiting_verification" -> {
                                            val subStatus = result.get("subStatus")?.asString
                                            statusMessage = when (subStatus) {
                                                "clicking" -> "Confirmando que você é humano..."
                                                "success" -> "Sucesso na verificação! Prosseguindo..."
                                                else -> "Verificando segurança do portal..."
                                            }
                                            handler.postDelayed(this, 2000)
                                        }
                                        "waiting_user" -> {
                                            statusMessage = "Por favor, clique no botão para prosseguir."
                                            handler.postDelayed(this, 2000)
                                        }
                                        else -> {
                                            handler.postDelayed(this, 2000)
                                        }
                                    }
                                } catch (e: Exception) {
                                    handler.postDelayed(this, 2000)
                                }
                            }
                        }
                    }
                    handler.post(checkContent)
                }
                
                override fun onReceivedError(view: WebView?, request: android.webkit.WebResourceRequest?, error: android.webkit.WebResourceError?) {
                    super.onReceivedError(view, request, error)
                    statusMessage = "Erro de conexão com o portal."
                }
            }
            loadUrl(url)
        }
    }

    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (isLoading && !showWebView) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            if (!showWebView) {
                Text(statusMessage, style = MaterialTheme.typography.titleMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                Text("Ação manual necessária no portal:", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
            }

            AndroidView(
                modifier = if (showWebView) Modifier.fillMaxWidth().weight(1f) else Modifier.size(0.dp).alpha(0f),
                factory = { webView }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onCancel) { Text("Cancelar") }
        }
    }
}

@Composable
fun NfceProcessorView(
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
            Log.d("NfceProcessor", "Handling supplier: CNPJ=$cnpj, Name=$name")
            
            var supplier = supabaseService.getSupplierByCnpj("eq.$cnpj").firstOrNull()
            Log.d("NfceProcessor", "Search by CNPJ result: ${supplier?.fullName ?: "Not found"}")
            
            if (supplier == null) {
                supplier = supabaseService.getSupplierByFullname("eq.$name").firstOrNull()
                Log.d("NfceProcessor", "Search by Name result: ${supplier?.fullName ?: "Not found"}")
            }
            
            if (supplier == null) {
                statusMessage = "Cadastrando novo fornecedor..."
                Log.d("NfceProcessor", "Creating new supplier...")
                val resp = supabaseService.createSupplier(
                    supplier = mapOf(
                        "fullname" to name,
                        "cpfcnpj" to cnpj,
                        "address" to (data["supplier_address"] ?: "")
                    )
                )
                supplierId = resp.body()?.firstOrNull()?.id ?: -1
                Log.d("NfceProcessor", "New supplier created ID: $supplierId")
            } else {
                supplierId = supplier.id ?: -1
                Log.d("NfceProcessor", "Existing supplier found ID: $supplierId")
            }

            if (supplierId == -1) {
                statusMessage = "Erro ao identificar/cadastrar fornecedor."
                Log.e("NfceProcessor", "Failed to get supplier ID")
                isProcessing = false
                return@LaunchedEffect
            }

            // Fetch products for the combobox
            statusMessage = "Carregando produtos..."
            Log.d("NfceProcessor", "Fetching all products...")
            allProducts = supabaseService.getProducts()
            Log.d("NfceProcessor", "Fetched ${allProducts.size} products")
            
            isProcessing = false
        } catch (e: Exception) {
            Log.e("NfceProcessor", "Error in initial processing", e)
            statusMessage = "Erro: ${e.message}"
            isProcessing = false
        }
    }

    LaunchedEffect(currentItemIndex, supplierId, showLinkDialog, isProcessing) {
        if (supplierId == -1 || currentItemIndex >= items.size || showLinkDialog || isProcessing) return@LaunchedEffect
        
        val item = items[currentItemIndex]
        val code = (item["product_code"] as? String) ?: ""
        val name = (item["product_name"] as? String) ?: ""
        Log.d("NfceProcessor", "Processing item $currentItemIndex: $name (Code: $code)")

        try {
            Log.d("NfceProcessor", "Searching for mapping: supplierId=$supplierId, code=$code, name=$name")
            val mapping = supabaseService.getProductXSuppliers(
                supplierId = "eq.$supplierId",
                code = if (code.isNotEmpty()) "eq.$code" else null,
                description = if (code.isEmpty()) "eq.$name" else null
            ).firstOrNull()

            if (mapping != null) {
                Log.d("NfceProcessor", "Mapping found: productId=${mapping.productId}")
                processedItems.add(item + mapOf("product_id" to mapping.productId!!))
                currentItemIndex++
            } else {
                Log.d("NfceProcessor", "No mapping found for item")
                showLinkDialog = true
            }
        } catch (e: Exception) {
             Log.e("NfceProcessor", "Error checking mapping", e)
        }

        if (currentItemIndex >= items.size && !showLinkDialog) {
            Log.d("NfceProcessor", "All items processed successfully")
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
                            Log.d("NfceProcessor", "Saving manual mapping: supplierId=$supplierId, productId=${product.id}, code=${currentItem["product_code"]}, description=${currentItem["product_name"]}")
                            try {
                                supabaseService.createProductXSupplier(
                                    data = mapOf(
                                        "supplier_id" to supplierId,
                                        "product_id" to product.id!!,
                                        "code" to (currentItem["product_code"] ?: ""),
                                        "description" to (currentItem["product_name"] ?: "")
                                    )
                                )
                                Log.d("NfceProcessor", "Manual mapping saved")
                                processedItems.add(currentItem + mapOf("product_id" to product.id))
                                showLinkDialog = false
                                selectedProductForLinking = null
                                searchQuery = ""
                                currentItemIndex++
                            } catch (e: Exception) {
                                Log.e("NfceProcessor", "Error saving manual mapping", e)
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
                            Log.d("NfceProcessor", "Creating new product and mapping for item: ${currentItem["product_name"]}")
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
                                    Log.d("NfceProcessor", "New product created ID: ${newProduct.id}")
                                    supabaseService.createProductXSupplier(
                                        data = mapOf(
                                            "supplier_id" to supplierId,
                                            "product_id" to newProduct.id!!,
                                            "code" to (currentItem["product_code"] ?: ""),
                                            "description" to (currentItem["product_name"] ?: "")
                                        )
                                    )
                                    Log.d("NfceProcessor", "Mapping for new product saved")
                                    processedItems.add(currentItem + mapOf("product_id" to newProduct.id))
                                    
                                    // Refresh products list
                                    allProducts = supabaseService.getProducts()

                                    showLinkDialog = false
                                    selectedProductForLinking = null
                                    searchQuery = ""
                                    currentItemIndex++
                                } else {
                                    Log.e("NfceProcessor", "Failed to get new product representation")
                                }
                            } catch (e: Exception) {
                                Log.e("NfceProcessor", "Error creating product and mapping", e)
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

    if (isProcessing) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(statusMessage)
            }
        }
    }
}

fun parseNfceHtml(html: String): Map<String, Any>? {
    Log.d("NfceParser", "Starting HTML parsing...")
    val doc = Jsoup.parse(html)
    
    val supplierName = doc.select(".txtTopo").first()?.text() 
        ?: doc.select("#conteudo .txtCenter").first()?.text() ?: ""
    Log.d("NfceParser", "Supplier Name: $supplierName")
    
    // Attempt to extract the entire info block to search for CNPJ and Address
    val infoBlock = doc.select("#conteudo .text").text().ifEmpty { 
        doc.select("#conteudo").text() 
    }
    
    val supplierCnpj = Regex("""CNPJ:\s*([\d./-]+)""").find(infoBlock)?.groupValues?.get(1) ?: ""
    Log.d("NfceParser", "Supplier CNPJ: $supplierCnpj")

    val supplierAddress = if (infoBlock.contains("Endereço:")) {
        infoBlock.substringAfter("Endereço:").substringBefore("CNPJ:").trim()
    } else ""
    Log.d("NfceParser", "Supplier Address: $supplierAddress")

    val items = mutableListOf<Map<String, Any>>()
    val rows = doc.select("tr[id^=Item +]")
    Log.d("NfceParser", "Found ${rows.size} item rows (standard)")
    
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
        Log.d("NfceParser", "Standard parsing failed, trying fallback...")
        val table = doc.select("table#tabResult").first()
        if (table != null) {
            val rowsFallback = table.select("tr")
            Log.d("NfceParser", "Found ${rowsFallback.size} fallback rows")
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

    Log.d("NfceParser", "Parsing finished. Found ${items.size} items.")
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
