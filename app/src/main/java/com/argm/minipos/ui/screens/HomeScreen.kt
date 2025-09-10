package com.argm.minipos.ui.screens

import androidx.compose.foundation.layout.Arrangement // Necesario para Column
import androidx.compose.foundation.layout.Column // Necesario para el layout vertical
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth // Para que los botones ocupen el ancho
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Sync // Icono para Sincronización (opcional)
import androidx.compose.material.icons.outlined.AccountBalanceWallet // Icono para Depósito (opcional)
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment // Necesario para Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.argm.minipos.ui.navigation.AppScreens
import com.argm.minipos.ui.viewmodel.HomeViewModel
import com.argm.minipos.ui.widgets.home.HomeScreenMainContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    if (uiState.errorMessage != null) {
        LaunchedEffect(uiState.errorMessage) {
            snackbarHostState.showSnackbar(
                message = uiState.errorMessage!!,
                duration = SnackbarDuration.Short
            )
            viewModel.clearErrorMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("MiniPOS - Inicio") },
                actions = {
                    IconButton(onClick = { navController.navigate(AppScreens.SALES_HISTORY_SCREEN) }) {
                        Icon(Icons.Rounded.History, contentDescription = "Historial de Ventas")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.addSampleProduct() }) {
                Icon(Icons.Filled.Add, contentDescription = "Añadir Producto de Muestra")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {

            HomeScreenMainContent(
                modifier = Modifier.fillMaxWidth(),
                uiState = uiState,
                navController = navController
            )
        }
    }
}

