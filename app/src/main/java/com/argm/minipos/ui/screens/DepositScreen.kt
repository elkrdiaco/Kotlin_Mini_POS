package com.argm.minipos.ui.screens // o com.argm.minipos.ui.screens.deposit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.argm.minipos.ui.navigation.AppScreens
import com.argm.minipos.ui.theme.MiniPOSTheme
import com.argm.minipos.ui.viewmodel.DepositViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepositScreen(
    navController: NavController,
    depositViewModel: DepositViewModel = hiltViewModel()
) {
    val uiState by depositViewModel.uiState.collectAsState()
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val localUiState = uiState // para evitar capturar uiState directamente en LaunchedEffect

    LaunchedEffect(localUiState.message) {
        if (localUiState.message != null && !localUiState.isError) {
            delay(3000)
            depositViewModel.clearMessage()
        }
    }

    LaunchedEffect(key1 = lifecycleOwner, key2 = navController.currentBackStackEntry) {
        navController.currentBackStackEntry?.savedStateHandle?.getLiveData<String>(SELECTED_CUSTOMER_RUT_KEY)
            ?.observe(lifecycleOwner) { rut ->
                if (rut != null) {
                    depositViewModel.onCustomerSelected(rut)
                    navController.currentBackStackEntry?.savedStateHandle?.remove<String>(SELECTED_CUSTOMER_RUT_KEY)
                }
            }
    }

    MiniPOSTheme {
        Scaffold(
            topBar = {
                TopAppBar(title = { Text("Realizar Depósito a Cliente") })
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(16.dp)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (uiState.selectedCustomerRut != null) {
                    Text("Cliente: ${uiState.selectedCustomerName ?: uiState.selectedCustomerRut}")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            navController.navigate(AppScreens.CUSTOMER_LIST_SCREEN)
                        }) {
                            Text("Cambiar")
                        }
                        Button(onClick = { depositViewModel.clearCustomerSelection() }) {
                            Text("Limpiar")
                        }
                    }
                } else {
                    Button(onClick = {
                        navController.navigate(AppScreens.CUSTOMER_LIST_SCREEN)
                    }) {
                        Text("Seleccionar Cliente")
                    }
                }

                OutlinedTextField(
                    value = uiState.amount,
                    onValueChange = { depositViewModel.onAmountChange(it) },
                    label = { Text("Monto del Depósito") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = uiState.isError,
                    supportingText = {
                        if (uiState.isError && uiState.message != null) {
                            Text(uiState.message!!, color = MaterialTheme.colorScheme.error)
                        }
                    }
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Simular Conexión Online")
                    Switch(
                        checked = uiState.isOnline,
                        onCheckedChange = { depositViewModel.onOnlineStatusChange(it) }
                    )
                }
                Button(
                    onClick = { depositViewModel.performDeposit() },
                    enabled = !uiState.isLoading && uiState.selectedCustomerRut != null && uiState.amount.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Depositar a Cliente")
                    }
                }

                if (uiState.depositSuccess && uiState.message != null) {
                    Text(
                        text = uiState.message!!,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DepositScreenPreview() {
    Text("Preview no disponible para esta pantalla completa con NavController y ViewModel.")
}
