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
                    .height(400.dp)
                    .padding(top = 16.dp), 
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.databaseEnabled = true
                        settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36"
                        
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, currentUrl: String?) {
                                super.onPageFinished(view, currentUrl)
                                debugLog = "Página carregada em: $currentUrl"
                                
                                val handler = android.os.Handler(android.os.Looper.getMainLooper())
                                val checkContent = object : Runnable {
                                    override fun run() {
                                        if (!isLoading) return
                                        
                                        view?.evaluateJavascript(
                                            """
                                            (function() {
                                                // 1. Check if we are already on the data page
                                                var hasData = !!document.querySelector('.txtTit') || 
                                                              !!document.querySelector('.txtTopo') ||
                                                              document.body.innerText.includes("DOCUMENTO AUXILIAR DA NOTA FISCAL DE CONSUMIDOR ELETRÔNICA");
                                                if (hasData) {
                                                    return { status: "ready", html: document.documentElement.outerHTML };
                                                }

                                                // 2. Check for verification status
                                                var bodyText = document.body.innerText;
                                                if (bodyText.includes("Verificando...")) {
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
                                                            break;
                                                        }
                                                    }
                                                }

                                                // Only click if not disabled (verification "Sucesso!" usually enables it)
                                                if (btn && !btn.disabled) {
                                                    btn.click();
                                                    return { status: "clicked" };
                                                }
                                                
                                                return { status: "waiting" };
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
                                                        val unescapedHtml = html.replace("\\\\u003C", "<")
                                                            .replace("\\\\\"", "\"")
                                                            .replace("\\\\n", "\n")
                                                        
                                                        val data = parseNfceHtml(unescapedHtml)
                                                        if (data != null) {
                                                            isLoading = false
                                                            onDataExtracted(data)
                                                        } else {
                                                            statusMessage = "Erro ao interpretar dados da nota."
                                                            debugLog = "HTML recebido, mas campos não identificados."
                                                        }
                                                    }
                                                    "clicked" -> {
                                                        statusMessage = "Acessando nota fiscal..."
                                                        handler.postDelayed(this, 2000)
                                                    }
                                                    "waiting_verification" -> {
                                                        statusMessage = "Verificando segurança do portal..."
                                                        handler.postDelayed(this, 1000)
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
    val doc = Jsoup.parse(html)
    
    val supplierName = doc.select(".txtTopo").first()?.text() 
        ?: doc.select("#conteudo .txtCenter").first()?.text() ?: ""
    
    val infoText = doc.select("#conteudo .text").text()
    
    val supplierCnpj = Regex("CNPJ:\\\\s*([\\\\d./-]+)").find(infoText)?.groupValues?.get(1) ?: ""
    val supplierAddress = doc.select("#conteudo .text").asSequence()
        .filter { it.text().contains("Endereço:") }
        .firstOrNull()?.text()
        ?.substringAfter("Endereço:")?.trim() ?: ""

    val items = mutableListOf<Map<String, Any>>()
    val rows = doc.select("tr[id^=Item +]")
    
    rows.forEach { row ->
        val name = row.select(".txtTit").text()
        val details = row.select(".R_Text").text() 
        
        val qty = Regex("Qtde\\.:\\\\s*([\\\\d,.]+)").find(details)?.groupValues?.get(1)?.replace(".", "")?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
        val unit = Regex("UN:\\\\s*(\\\\w+)").find(details)?.groupValues?.get(1) ?: ""
        val price = Regex("Vl\\\\. Unit\\.:\\\\s*([\\\\d,.]+)").find(details)?.groupValues?.get(1)?.replace(".", "")?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
        
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
        val table = doc.select("table#tabResult").first()
        if (table != null) {
            val rowsFallback = table.select("tr")
            rowsFallback.forEach { r ->
                val name = r.select("td.txtTit").text()
                if (name.isNotEmpty()) {
                    val details = r.select("td.R_Text").text()
                    val qty = Regex("Qtde\\.:\\\\s*([\\\\d,.]+)").find(details)?.groupValues?.get(1)?.replace(".", "")?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
                    val unit = Regex("UN:\\\\s*(\\\\w+)").find(details)?.groupValues?.get(1) ?: ""
                    val price = Regex("Vl\\\\. Unit\\.:\\\\s*([\\\\d,.]+)").find(details)?.groupValues?.get(1)?.replace(".", "")?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
                    items.add(mapOf(
                        "product_name" to name,
                        "product_unit" to unit,
                        "quantity" to qty,
                        "price" to price
                    ))
                }
            }
        }
    }

    if (supplierName.isEmpty() && items.isEmpty()) return null

    return mapOf(
        "supplier_name" to supplierName,
        "supplier_cnpj" to supplierCnpj,
        "supplier_address" to supplierAddress,
        "items" to items
    )
}
