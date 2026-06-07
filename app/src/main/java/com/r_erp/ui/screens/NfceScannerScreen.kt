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
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
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

    if (scannedUrl == null) {
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
    } else {
        NfceLoaderView(
            url = scannedUrl!!,
            onDataExtracted = onNfceDataExtracted,
            onCancel = { scannedUrl = null }
        )
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

@androidx.camera.core.ExperimentalGetImage
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
    var debugLog by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (isLoading) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
            }
            Text(statusMessage, style = MaterialTheme.typography.titleMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            
            if (debugLog.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(debugLog, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onCancel) { Text("Cancelar") }
            
            // WebView visible for inspection
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Changed from fixed height to weight to fill space
                    .padding(top = 16.dp), 
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.databaseEnabled = true
                        // Set a modern mobile User Agent
                        settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36"
                        
                        // Enable mixed content if necessary
                        settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        
                        webChromeClient = object : android.webkit.WebChromeClient() {
                            override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                                consoleMessage?.let {
                                    Log.d("NfceWebViewConsole", "${it.message()} -- From line ${it.lineNumber()} of ${it.sourceId()}")
                                }
                                return true
                            }
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                Log.d("NfceLoader", "Iniciando carregamento: $url")
                            }

                            override fun onPageFinished(view: WebView?, currentUrl: String?) {
                                super.onPageFinished(view, currentUrl)
                                Log.d("NfceLoader", "Página finalizada: $currentUrl")
                                debugLog = "Página carregada em: $currentUrl"
                                
                                val handler = android.os.Handler(android.os.Looper.getMainLooper())
                                val checkContent = object : Runnable {
                                    override fun run() {
                                        if (!isLoading) return
                                        
                                        view?.evaluateJavascript(
                                            """
                                            (function() {
                                                console.log("NFCe Debug: Running check script");
                                                
                                                // 0. Check for Cloudflare/Challenges
                                                if (document.title.includes("Just a moment") || 
                                                    document.querySelector('#challenge-running') || 
                                                    document.querySelector('.cf-browser-verification')) {
                                                    console.log("NFCe Debug: Cloudflare challenge detected");
                                                    return { status: "waiting_verification" };
                                                }

                                                // 1. Check if we are already on the data page
                                                var bodyText = document.body.innerText;
                                                var hasData = !!document.querySelector('.txtTit') || 
                                                              !!document.querySelector('.txtTopo') ||
                                                              bodyText.includes("DOCUMENTO AUXILIAR DA NOTA FISCAL DE CONSUMIDOR ELETRÔNICA") ||
                                                              bodyText.includes("Chave de Acesso");
                                                
                                                if (hasData) {
                                                    console.log("NFCe Debug: Data page detected. HTML length: " + document.documentElement.outerHTML.length);
                                                    return { status: "ready", html: document.documentElement.outerHTML };
                                                }

                                                // 2. Check for verification status
                                                var bodyText = document.body.innerText;
                                                if (bodyText.includes("Verificando...")) {
                                                    console.log("NFCe Debug: Verification in progress");
                                                    return { status: "waiting_verification" };
                                                }

                                                // 3. Try to find the specific button
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
                                                            console.log("NFCe Debug: Found button via text search: " + text);
                                                            break;
                                                        }
                                                    }
                                                }

                                                // Only click if not disabled (verification "Sucesso!" usually enables it)
                                                if (btn && !btn.disabled) {
                                                    // Check if we already clicked this recently to avoid IPC flooding
                                                    if (window._nfceClicked) {
                                                        console.log("NFCe Debug: Button already clicked, waiting for navigation...");
                                                        return { status: "clicked" };
                                                    }
                                                    
                                                    console.log("NFCe Debug: Clicking validation button");
                                                    window._nfceClicked = true;
                                                    
                                                    // Try standard click
                                                    btn.click();
                                                    
                                                    // Try form submission if it's a submit button
                                                    if (btn.type === "submit" && btn.form) {
                                                        btn.form.submit();
                                                    }
                                                    
                                                    // Try manual event dispatch
                                                    btn.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true, view: window }));

                                                    return { status: "clicked" };
                                                }
                                                
                                                console.log("NFCe Debug: Waiting for verification or data. Button exists: " + (!!btn) + ", Button disabled: " + (btn ? btn.disabled : "N/A"));
                                                return { status: "waiting" };
                                            })()
                                            """.trimIndent()
                                        ) { resultJson ->
                                            try {
                                                Log.d("NfceLoader", "JS Result: $resultJson")
                                                val result = com.google.gson.JsonParser.parseString(resultJson ?: "{}").asJsonObject
                                                val status = result.get("status").asString
                                                
                                                when (status) {
                                                    "ready" -> {
                                                        Log.d("NfceLoader", "Status: Ready. Starting HTML parse.")
                                                        statusMessage = "Dados encontrados! Processando..."
                                                        val html = result.get("html").asString
                                                        val unescapedHtml = html.replace("\\\\u003C", "<")
                                                            .replace("\\\\\"", "\"")
                                                            .replace("\\\\n", "\n")
                                                        
                                                        val data = parseNfceHtml(unescapedHtml)
                                                        if (data != null) {
                                                            Log.d("NfceLoader", "Parsing successful: ${data["supplier_name"]}")
                                                            isLoading = false
                                                            onDataExtracted(data)
                                                        } else {
                                                            Log.e("NfceLoader", "Parsing failed: Data is null")
                                                            statusMessage = "Erro ao interpretar dados da nota."
                                                            debugLog = "HTML recebido, mas campos não identificados."
                                                        }
                                                    }
                                                    "clicked" -> {
                                                        Log.d("NfceLoader", "Status: Clicked. Waiting for navigation.")
                                                        statusMessage = "Acessando nota fiscal..."
                                                        handler.postDelayed(this, 3000)
                                                    }
                                                    "waiting_verification" -> {
                                                        Log.d("NfceLoader", "Status: Waiting Verification.")
                                                        statusMessage = "Verificando segurança do portal..."
                                                        handler.postDelayed(this, 1000)
                                                    }
                                                    else -> {
                                                        Log.d("NfceLoader", "Status: Waiting/Other.")
                                                        handler.postDelayed(this, 2000)
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                Log.e("NfceLoader", "Error processing JS result", e)
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
                                debugLog = error?.description?.toString() ?: "Erro desconhecido"
                            }
                        }
                        loadUrl(url)
                    }
                }
            )
        }
    }
}

fun parseNfceHtml(html: String): Map<String, Any>? {
    Log.d("NfceParser", "Parsing HTML content. Length: ${html.length}")
    val doc = Jsoup.parse(html)
    
    val supplierName = doc.select(".txtTopo").first()?.text() 
        ?: doc.select("#conteudo .txtCenter").first()?.text() ?: ""
    Log.d("NfceParser", "Supplier Name: $supplierName")
    
    // Attempt to extract the entire info block to search for CNPJ and Address
    val infoBlock = doc.select("#conteudo .text").text().ifEmpty { 
        doc.select("#conteudo").text() 
    }
    Log.d("NfceParser", "Info block text: $infoBlock")
    
    val supplierCnpj = Regex("""CNPJ:\s*([\d./-]+)""").find(infoBlock)?.groupValues?.get(1) ?: ""
    Log.d("NfceParser", "Supplier CNPJ: $supplierCnpj")

    val supplierAddress = if (infoBlock.contains("Endereço:")) {
        infoBlock.substringAfter("Endereço:").substringBefore("CNPJ:").trim()
    } else ""
    Log.d("NfceParser", "Supplier Address: $supplierAddress")

    val items = mutableListOf<Map<String, Any>>()
    val rows = doc.select("tr[id^=Item +]")
    Log.d("NfceParser", "Found ${rows.size} item rows via ID selector")
    
    rows.forEach { row ->
        val nameRaw = row.select(".txtTit").text()
        // Remove "Vl. Total..." from the name
        val name = nameRaw.substringBefore("Vl. Total").trim()
        
        // Try multiple selectors for details
        var details = row.select(".R_Text").text()
        if (details.isEmpty()) {
            // If .R_Text is empty, maybe it's a sibling or child TD
            details = row.select("td").joinToString(" ") { it.text() }
        }
        
        Log.d("NfceParser", "Item Name: $name | Details: $details")
        
        val qty = Regex("""Qtde\.:\s*([\d,.]+)""").find(details)?.groupValues?.get(1)?.replace(".", "")?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
        val unit = Regex("""UN:\s*(\w+)""").find(details)?.groupValues?.get(1) ?: ""
        val price = Regex("""Vl\. Unit\.:\s*([\d,.]+)""").find(details)?.groupValues?.get(1)?.replace(".", "")?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
        
        if (name.isNotEmpty()) {
            items.add(mapOf(
                "product_name" to name,
                "product_unit" to unit,
                "quantity" to qty,
                "price" to price
            ))
        }
    }

    if (supplierName.isEmpty() || items.isEmpty()) {
        Log.d("NfceParser", "Attempting fallback row detection")
        val table = doc.select("table#tabResult").first()
        if (table != null) {
            val rowsFallback = table.select("tr")
            Log.d("NfceParser", "Found ${rowsFallback.size} rows in tabResult table")
            rowsFallback.forEach { r ->
                val nameRaw = r.select("td.txtTit").text()
                if (nameRaw.isNotEmpty()) {
                    val name = nameRaw.substringBefore("Vl. Total").trim()
                    var details = r.select("td.R_Text").text()
                    if (details.isEmpty()) {
                        details = r.select("td").joinToString(" ") { it.text() }
                    }

                    Log.d("NfceParser", "Fallback Item Name: $name | Details: $details")
                    
                    val qty = Regex("""Qtde\.:\s*([\d,.]+)""").find(details)?.groupValues?.get(1)?.replace(".", "")?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
                    val unit = Regex("""UN:\s*(\w+)""").find(details)?.groupValues?.get(1) ?: ""
                    val price = Regex("""Vl\. Unit\.:\s*([\d,.]+)""").find(details)?.groupValues?.get(1)?.replace(".", "")?.replace(",", ".")?.toDoubleOrNull() ?: 0.0

                    items.add(mapOf(
                        "product_name" to name,
                        "product_unit" to unit,
                        "quantity" to qty,
                        "price" to price
                    ))
                }
            }
        } else {
            Log.d("NfceParser", "No tabResult table found")
        }
    }

    Log.d("NfceParser", "Total items extracted: ${items.size}")

    if (supplierName.isEmpty() && items.isEmpty()) {
        Log.e("NfceParser", "Failed to extract supplier name and items")
        return null
    }

    return mapOf(
        "supplier_name" to supplierName,
        "supplier_cnpj" to supplierCnpj,
        "supplier_address" to supplierAddress,
        "items" to items
    )
}
