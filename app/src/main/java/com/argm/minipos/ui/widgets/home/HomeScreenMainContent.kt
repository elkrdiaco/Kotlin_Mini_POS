package com.argm.minipos.ui.widgets.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.ProductionQuantityLimits
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.argm.minipos.ui.navigation.AppScreens
import com.argm.minipos.ui.viewmodel.HomeUiState

// Import ProductItem desde su nueva ubicación


@Composable
fun HomeScreenMainContent(
    modifier: Modifier = Modifier,
    uiState: HomeUiState,
    navController: NavController
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        }

        Button(
            onClick = { navController.navigate(AppScreens.DEPOSIT_SCREEN) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = "Depósito", modifier = Modifier.padding(end = 8.dp))
            Text("Ir a Depósitos")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { navController.navigate(AppScreens.SYNC_SCREEN) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Sync, contentDescription = "Sincronización", modifier = Modifier.padding(end = 8.dp))
            Text("Ir a Sincronización")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { navController.navigate(AppScreens.SALES_SCREEN) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Outlined.ProductionQuantityLimits, contentDescription = "Venta", modifier = Modifier.padding(end = 8.dp))
            Text("Realizar Venta")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Productos Recientes/Disponibles:", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(8.dp))

        if (uiState.products.isEmpty() && !uiState.isLoading) {
            Text("No hay productos disponibles. Añade algunos.")
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(uiState.products) { product ->
                    ProductItem(product)
                    HorizontalDivider(
                        Modifier,
                        DividerDefaults.Thickness,
                        DividerDefaults.color
                    )
                }
            }
        }
    }
}