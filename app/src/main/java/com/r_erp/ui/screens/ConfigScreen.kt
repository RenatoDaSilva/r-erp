package com.r_erp.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.r_erp.api.LocalSessionManager
import com.r_erp.api.LocalToken
import com.r_erp.api.SupabaseService
import kotlinx.coroutines.launch

@Composable
fun ConfigScreen(onBack: () -> Unit) {
    val token = LocalToken.current
    val sessionManager = LocalSessionManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var configId by remember { mutableStateOf<Int?>(null) }
    var companyName by remember { mutableStateOf("") }
    var companyAddress by remember { mutableStateOf("") }
    var cnpjCpf by remember { mutableStateOf("") }
    var defaultMessageBudget by remember { mutableStateOf("") }
    var defaultMessageOrder by remember { mutableStateOf("") }
    var logo by remember { mutableStateOf<String?>(null) }

    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val supabaseService = remember(token) { SupabaseService.create(token, sessionManager) }

    LaunchedEffect(supabaseService) {
        try {
            val configs = supabaseService.getConfig()
            if (configs.isNotEmpty()) {
                val cfg = configs.first()
                configId = cfg.id
                companyName = cfg.companyName ?: ""
                companyAddress = cfg.companyAddress ?: ""
                cnpjCpf = cfg.cnpjCpf ?: ""
                defaultMessageBudget = cfg.defaultMessageBudget ?: ""
                defaultMessageOrder = cfg.defaultMessageOrder ?: ""
                logo = cfg.logo
            }
            isLoading = false
        } catch (e: Exception) {
            errorMessage = e.message ?: e.toString()
            isLoading = false
        }
    }

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                try {
                    val inputStream = context.contentResolver.openInputStream(it)
                    val bytes = inputStream?.readBytes()
                    inputStream?.close()
                    if (bytes != null) {
                        // Store the actual ByteArray for bytea column
                        // Coil and Retrofit (via Gson/Supabase) handle ByteArrays differently.
                        // For Supabase 'bytea' via JSON, it usually expects a base64 string or hex.
                        // However, we'll keep it as a Base64 string in the state for now, 
                        // but ensure it's handled correctly during the API call.
                        logo = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                    }
                } catch (e: Exception) {
                    errorMessage = "Erro ao processar imagem: ${e.message}"
                }
            }
        }
    }

    val fields = listOf(
        Triple("Logo", "logo", "IMAGE"),
        Triple("Nome da Empresa", "company_name", "TEXT"),
        Triple("Endereço da Empresa", "company_address", "TEXT"),
        Triple("CNPJ/CPF", "cnpj_cpf", "TEXT"),
        Triple("Msg. Padrão Orçamento", "default_message_budget", "TEXT"),
        Triple("Msg. Padrão Pedido", "default_message_order", "TEXT")
    )

    val filteredFields = fields.filter { it.first.contains(searchQuery, ignoreCase = true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    isSaving = true
                    scope.launch {
                        try {
                            val configMap = mutableMapOf<String, Any>(
                                "company_name" to companyName,
                                "company_address" to companyAddress,
                                "cnpj_cpf" to cnpjCpf,
                                "default_message_budget" to defaultMessageBudget,
                                "default_message_order" to defaultMessageOrder
                            )
                            // Handle the 'bytea' field. Postgres 'bytea' via Supabase REST (PostgREST)
                            // expects a Hex string with \x prefix when using JSON.
                            logo?.let { 
                                if (it.isNotEmpty() && !it.startsWith("content://") && !it.startsWith("http")) {
                                    try {
                                        val bytes = android.util.Base64.decode(it, android.util.Base64.DEFAULT)
                                        val hexChars = "0123456789abcdef"
                                        val hexString = StringBuilder("\\x")
                                        for (b in bytes) {
                                            val i = b.toInt() and 0xff
                                            hexString.append(hexChars[i shr 4])
                                            hexString.append(hexChars[i and 0x0f])
                                        }
                                        configMap["logo"] = hexString.toString()
                                    } catch (e: Exception) {
                                        // If already hex or other format, skip conversion
                                    }
                                }
                            }

                            val response = if (configId != null) {
                                supabaseService.updateConfig("eq.$configId", configMap)
                            } else {
                                val userId = SupabaseService.getUserIdFromToken(token ?: "")
                                if (userId != null) {
                                    configMap["user_id"] = userId
                                }
                                supabaseService.createConfig(configMap)
                            }

                            if (response.isSuccessful) {
                                Toast.makeText(context, "Configurações salvas!", Toast.LENGTH_SHORT).show()
                                onBack()
                            } else {
                                errorMessage = "Erro ao salvar: ${response.code()} ${response.message()}\n${response.errorBody()?.string()}"
                            }
                        } catch (e: Exception) {
                            errorMessage = e.message ?: e.toString()
                        } finally {
                            isSaving = false
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Salvar")
                }
            }
            Button(
                onClick = onBack,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                enabled = !isSaving
            ) {
                Text("Cancelar")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth(),
            placeholder = { Text("Filtrar campos...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Limpar")
                    }
                }
            },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredFields) { field ->
                        val (label, key, type) = field
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.titleSmall
                                )
                                
                                Box(modifier = Modifier.weight(1.5f)) {
                                    when (type) {
                                        "IMAGE" -> {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                if (!logo.isNullOrEmpty()) {
                                                    val imageModel = remember(logo) {
                                                        if (logo.isNullOrEmpty()) return@remember null
                                                        if (logo!!.startsWith("content://") || logo!!.startsWith("http")) {
                                                            logo
                                                        } else if (logo!!.startsWith("\\x")) {
                                                            // Decode Postgres Hex format
                                                            try {
                                                                val hex = logo!!.substring(2)
                                                                val bytes = ByteArray(hex.length / 2)
                                                                for (i in 0 until hex.length step 2) {
                                                                    bytes[i / 2] = hex.substring(i, i + 2).toInt(16).toByte()
                                                                }
                                                                bytes
                                                            } catch (e: Exception) {
                                                                null
                                                            }
                                                        } else {
                                                            try {
                                                                android.util.Base64.decode(logo, android.util.Base64.DEFAULT)
                                                            } catch (e: Exception) {
                                                                logo
                                                            }
                                                        }
                                                    }
                                                    AsyncImage(
                                                        model = imageModel,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(100.dp),
                                                        contentScale = ContentScale.Fit
                                                    )
                                                }
                                                Button(onClick = { imageLauncher.launch("image/*") }) {
                                                    Text("Carregar Imagem")
                                                }
                                            }
                                        }
                                        "TEXT" -> {
                                            val value = when (key) {
                                                "company_name" -> companyName
                                                "company_address" -> companyAddress
                                                "cnpj_cpf" -> cnpjCpf
                                                "default_message_budget" -> defaultMessageBudget
                                                "default_message_order" -> defaultMessageOrder
                                                else -> ""
                                            }
                                            OutlinedTextField(
                                                value = value,
                                                onValueChange = { newValue ->
                                                    when (key) {
                                                        "company_name" -> companyName = newValue
                                                        "company_address" -> companyAddress = newValue
                                                        "cnpj_cpf" -> cnpjCpf = newValue
                                                        "default_message_budget" -> defaultMessageBudget = newValue
                                                        "default_message_order" -> defaultMessageOrder = newValue
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}
