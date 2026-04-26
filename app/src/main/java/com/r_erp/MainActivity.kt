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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.LocalMall
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.r_erp.ui.theme.RerpTheme
import com.r_erp.ui.screens.ClientsScreen
import com.r_erp.ui.screens.ClientDetailScreen
import com.r_erp.ui.screens.SuppliersScreen
import com.r_erp.ui.screens.SupplierDetailScreen
import com.r_erp.ui.screens.AgendaScreen
import com.r_erp.ui.screens.AddAgendaItemScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RerpTheme {
                MainScreen()
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
fun MainScreen() {
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
    )
    var selectedItemIndex by rememberSaveable { mutableIntStateOf(0) }
    var selectedId by rememberSaveable { mutableStateOf<Int?>(null) }
    var isAddingAgendaItem by rememberSaveable { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))
                items.forEachIndexed { index, item ->
                    NavigationDrawerItem(
                        label = { Text(text = item.title) },
                        selected = index == selectedItemIndex,
                        onClick = {
                            selectedItemIndex = index
                            selectedId = null
                            isAddingAgendaItem = false
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
            }
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        val title = when {
                            isAddingAgendaItem -> "Novo Agendamento"
                            selectedId != null && items[selectedItemIndex].title == "Clientes" -> "Dados do Cliente"
                            selectedId != null && items[selectedItemIndex].title == "Fornecedores" -> "Dados do Fornecedor"
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

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    RerpTheme {
        MainScreen()
    }
}
