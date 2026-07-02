package com.r_erp.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.r_erp.api.LocalSessionManager
import com.r_erp.api.LocalToken
import com.r_erp.api.SupabasePeriod
import com.r_erp.api.SupabaseService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun RoutinesScreen() {
    val token = LocalToken.current
    val sessionManager = LocalSessionManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val supabaseService = remember(token) { SupabaseService.create(token, sessionManager) }

    var period by remember { mutableStateOf<SupabasePeriod?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }

    fun loadPeriod(showSpinner: Boolean = true) {
        if (showSpinner) isLoading = true
        scope.launch {
            try {
                period = supabaseService.oldestPeriod(emptyMap())
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    val msg = e.message ?: e.toString()
                    if (!msg.contains("composition", ignoreCase = true)) {
                        Toast.makeText(context, "Erro ao carregar período: $msg", Toast.LENGTH_SHORT).show()
                    }
                }
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(supabaseService) {
        loadPeriod(showSpinner = period == null)
    }

    val hasPeriod = period?.month != null && period?.year != null
    val buttonCaption = if (hasPeriod) {
        "Encerrar mês ${period!!.month}/${period!!.year}"
    } else if (isLoading) {
        "Carregando..."
    } else {
        "Não há períodos para encerrar"
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        if (isLoading && period == null) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = { showConfirmDialog = true },
                    enabled = hasPeriod && !isProcessing,
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .height(100.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text(
                            text = buttonCaption,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    if (showConfirmDialog && period != null) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirmar Encerramento") },
            text = { Text("Estou prestes a encerrar o mês ${period!!.month} / ${period!!.year}. Você confirma essa ação?") },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    isProcessing = true
                    scope.launch {
                        try {
                            val response = supabaseService.closeMonth(emptyMap())
                            val message = if (response.isSuccessful) {
                                response.body() ?: "Mês encerrado com sucesso!"
                            } else {
                                "Erro ao encerrar: ${response.errorBody()?.string()}"
                            }
                            
                            // Show toast for ~10 seconds by showing it multiple times
                            // or just use a custom one. Standard Toast.LENGTH_LONG is ~3.5s.
                            // We'll show it 3 times to get close to 10s.
                            repeat(3) {
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                delay(3500)
                            }
                            
                            loadPeriod()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
                        } finally {
                            isProcessing = false
                        }
                    }
                }) {
                    Text("Sim")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Não")
                }
            }
        )
    }
}
