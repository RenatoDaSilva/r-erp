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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
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
        NfeProcessorView(
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
