package com.r_erp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.r_erp.api.LocalToken
import com.r_erp.api.LocalSessionManager
import com.r_erp.api.SupabaseService
import com.r_erp.ui.theme.RerpTheme
import com.r_erp.ui.screens.*
import com.r_erp.utils.SessionManager
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = SessionManager(this)
        enableEdgeToEdge()
        setContent {
            RerpTheme {
                val token by sessionManager.authToken.collectAsState(initial = "")
                
                if (token == null) {
                    SupabaseService.currentToken = null
                    LoginScreen(sessionManager) {
                        // Success handled by Flow
                    }
                } else if (token != "") {
                    SupabaseService.currentToken = token
                    CompositionLocalProvider(
                        LocalToken provides token!!,
                        LocalSessionManager provides sessionManager
                    ) {
                        MainScreen(sessionManager)
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

data class NavigationItem(
    val title: String,
    val icon: ImageVector,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(sessionManager: SessionManager) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val items = listOf(
        NavigationItem("Agenda", Icons.Default.Event),
        NavigationItem("Clientes", Icons.Default.Person),
        NavigationItem("Fornecedores", Icons.Default.Business),
        NavigationItem("Produtos", Icons.Default.ShoppingCart),
        NavigationItem("Serviços", Icons.Default.Build),
        NavigationItem("Compras", Icons.Default.LocalMall),
        NavigationItem("Orçamentos", Icons.Default.Description),
        NavigationItem("Pedidos", Icons.AutoMirrored.Filled.Assignment),
        NavigationItem("Receber", Icons.Default.AttachMoney),
        NavigationItem("Pagar", Icons.Default.AttachMoney),
        NavigationItem("A comprar", Icons.Default.ShoppingCart),
        NavigationItem("Configurações", Icons.Default.Settings),
    )
    var selectedItemIndex by rememberSaveable { mutableIntStateOf(0) }
    var selectedId by rememberSaveable { mutableStateOf<Int?>(null) }
    var isAddingAgendaItem by rememberSaveable { mutableStateOf(false) }
    var isAddingBudget by rememberSaveable { mutableStateOf(false) }
    var isAddingOrder by rememberSaveable { mutableStateOf(false) }
    var isAddingPurchase by rememberSaveable { mutableStateOf(false) }
    var isAddingReceivable by rememberSaveable { mutableStateOf(false) }
    var isAddingPayable by rememberSaveable { mutableStateOf(false) }
    var isAddingToBuy by rememberSaveable { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))
                items.filter { it.title != "Configurações" }.forEachIndexed { index, item ->
                    NavigationDrawerItem(
                        label = { Text(text = item.title) },
                        selected = index == selectedItemIndex && items[selectedItemIndex].title != "Configurações",
                        onClick = {
                            selectedItemIndex = index
                            selectedId = null
                            isAddingAgendaItem = false
                            isAddingBudget = false
                            isAddingOrder = false
                            isAddingPurchase = false
                            isAddingReceivable = false
                            isAddingPayable = false
                            isAddingToBuy = false
                            scope.launch {
                                drawerState.close()
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                            )
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))

                val configItemIndex = items.indexOfFirst { it.title == "Configurações" }
                if (configItemIndex != -1) {
                    val configItem = items[configItemIndex]
                    NavigationDrawerItem(
                        label = { Text(text = configItem.title) },
                        selected = selectedItemIndex == configItemIndex,
                        onClick = {
                            selectedItemIndex = configItemIndex
                            selectedId = null
                            isAddingAgendaItem = false
                            isAddingBudget = false
                            isAddingOrder = false
                            isAddingPurchase = false
                            isAddingReceivable = false
                            isAddingPayable = false
                            isAddingToBuy = false
                            scope.launch {
                                drawerState.close()
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = configItem.icon,
                                contentDescription = configItem.title,
                            )
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    )
                }
                
                NavigationDrawerItem(
                    label = { Text(text = "Sair") },
                    selected = false,
                    onClick = {
                        scope.launch {
                            sessionManager.clearSession()
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Sair",
                        )
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        val title = when {
                            isAddingAgendaItem -> "Novo Agendamento"
                            isAddingBudget -> "Novo Orçamento"
                            isAddingOrder -> "Novo Pedido"
                            isAddingPurchase -> "Nova Compra"
                            isAddingReceivable -> "Novo Recebimento"
                            isAddingPayable -> "Novo Pagamento"
                            isAddingToBuy -> "Adicionar a Comprar"
                            selectedId != null && items[selectedItemIndex].title == "Clientes" -> "Dados do Cliente"
                            selectedId != null && items[selectedItemIndex].title == "Fornecedores" -> "Dados do Fornecedor"
                            selectedId != null && items[selectedItemIndex].title == "Produtos" -> "Dados do Produto"
                            selectedId != null && items[selectedItemIndex].title == "Serviços" -> "Dados do Serviço"
                            selectedId != null && items[selectedItemIndex].title == "Pedidos" -> "Dados do Pedido"
                            selectedId != null && items[selectedItemIndex].title == "Compras" -> "Dados da Compra"
                            selectedId != null && items[selectedItemIndex].title == "Receber" -> "Dados do Título"
                            selectedId != null && items[selectedItemIndex].title == "Pagar" -> "Dados do Título"
                            items[selectedItemIndex].title == "Configurações" -> "Configurações"
                            else -> items[selectedItemIndex].title
                        }
                        Text(text = title)
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch {
                                drawerState.open()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu",
                            )
                        }
                    },
                )
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                when (items[selectedItemIndex].title) {
                    "Agenda" -> {
                        if (isAddingAgendaItem) {
                            AddAgendaItemScreen(onBack = { isAddingAgendaItem = false })
                        } else {
                            AgendaScreen(onAddAgendaItem = { isAddingAgendaItem = true })
                        }
                    }

                    "Clientes" -> {
                        if (selectedId != null) {
                            ClientDetailScreen(clientId = selectedId!!) {
                                selectedId = null
                            }
                        } else {
                            ClientsScreen(onClientClick = { id -> selectedId = id })
                        }
                    }

                    "Fornecedores" -> {
                        if (selectedId != null) {
                            SupplierDetailScreen(supplierId = selectedId!!) {
                                selectedId = null
                            }
                        } else {
                            SuppliersScreen(onSupplierClick = { id -> selectedId = id })
                        }
                    }

                    "Produtos" -> {
                        if (selectedId != null) {
                            ProductDetailScreen(productId = selectedId!!) {
                                selectedId = null
                            }
                        } else {
                            ProductsScreen(onProductClick = { id -> selectedId = id })
                        }
                    }

                    "Serviços" -> {
                        if (selectedId != null) {
                            ServiceDetailScreen(serviceId = selectedId!!) {
                                selectedId = null
                            }
                        } else {
                            ServicesScreen(onServiceClick = { id -> selectedId = id })
                        }
                    }

                    "Compras" -> {
                        if (isAddingPurchase || selectedId != null) {
                            PurchasesDetailScreen(
                                purchaseId = selectedId,
                                onBack = {
                                    isAddingPurchase = false
                                    selectedId = null
                                }
                            )
                        } else {
                            PurchasesScreen(
                                onAddPurchase = { isAddingPurchase = true },
                                onPurchaseClick = { id -> selectedId = id }
                            )
                        }
                    }

                    "Orçamentos" -> {
                        if (isAddingBudget || selectedId != null) {
                            BudgetDetailsScreen(
                                budgetId = selectedId,
                                onBack = { 
                                    isAddingBudget = false
                                    selectedId = null
                                }
                            )
                        } else {
                            BudgetsScreen(
                                onAddBudget = { isAddingBudget = true },
                                onBudgetClick = { id -> selectedId = id }
                            )
                        }
                    }

                    "Pedidos" -> {
                        if (isAddingOrder || selectedId != null) {
                            OrderDetailsScreen(
                                orderId = selectedId,
                                onBack = {
                                    isAddingOrder = false
                                    selectedId = null
                                }
                            )
                        } else {
                            OrdersScreen(
                                onAddOrder = { isAddingOrder = true },
                                onOrderClick = { id -> selectedId = id }
                            )
                        }
                    }

                    "Receber" -> {
                        if (isAddingReceivable || selectedId != null) {
                            ReceivableDetailsScreen(
                                receivableId = selectedId ?: -1,
                                onBack = {
                                    isAddingReceivable = false
                                    selectedId = null
                                }
                            )
                        } else {
                            ReceivablesScreen(
                                onAddReceivable = { isAddingReceivable = true },
                                onReceivableClick = { id -> selectedId = id }
                            )
                        }
                    }

                    "Pagar" -> {
                        if (isAddingPayable || selectedId != null) {
                            PayableDetailsScreen(
                                payableId = selectedId ?: -1,
                                onBack = {
                                    isAddingPayable = false
                                    selectedId = null
                                }
                            )
                        } else {
                            PayablesScreen(
                                onAddPayable = { isAddingPayable = true },
                                onPayableClick = { id -> selectedId = id }
                            )
                        }
                    }

                    "A comprar" -> {
                        if (isAddingToBuy) {
                            ToBuyDetailScreen(onBack = { isAddingToBuy = false })
                        } else {
                            ToBuyScreen(onAddToBuy = { isAddingToBuy = true })
                        }
                    }

                    "Configurações" -> {
                        ConfigScreen(onBack = {
                            selectedItemIndex = 0
                        })
                    }

                    else -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "Tela de ${items[selectedItemIndex].title}")
                        }
                    }
                }
            }
        }
    }
}
