package com.argm.minipos.ui.screens

import CartSection
import SaleSuccessDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.argm.minipos.ui.viewmodel.SalesViewModel
import com.argm.minipos.ui.widgets.sales.ProductList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesScreen(
    navController: NavController,
    viewModel: SalesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage, uiState.infoMessage, uiState.showSaleSuccessDialog) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(message = it, duration = SnackbarDuration.Short)
            viewModel.clearErrorMessage()
        }
        if (!uiState.showSaleSuccessDialog && uiState.infoMessage != null) {
            snackbarHostState.showSnackbar(message = uiState.infoMessage!!, duration = SnackbarDuration.Short)
            viewModel.clearInfoMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(title = { Text("MiniPOS - Ventas") })
        },
        bottomBar = {
            if (uiState.cart.isNotEmpty()) {
                BottomAppBar(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Total: $${"%.2f".format(uiState.cartTotal)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Button(onClick = {
                            viewModel.finalizeCurrentSale()
                        }) {
                            Text("Finalizar Venta")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Column(modifier = Modifier.weight(0.55f)) {
                Text(
                    "Productos Disponibles",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                if (uiState.isLoading && uiState.products.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (uiState.products.isEmpty() && !uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No hay productos disponibles.")
                    }
                } else {
                    ProductList(
                        products = uiState.products,
                        onProductClick = { product ->
                            viewModel.addProductToCart(product)
                        },
                        modifier = Modifier.fillMaxHeight()
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            CartSection(
                modifier = Modifier.weight(0.45f),
                cartItems = uiState.cart,
                cartTotal = uiState.cartTotal,
                onRemoveItem = { cartItem -> viewModel.removeProductFromCart(cartItem) },
                onUpdateQuantity = { product, quantity -> viewModel.updateCartItemQuantity(product, quantity) }
            )
        }
    }

    if (uiState.showSaleSuccessDialog && uiState.infoMessage != null) {
        SaleSuccessDialog(
            successMessage = uiState.infoMessage!!,
            onDismiss = { viewModel.dismissSaleSuccessDialog() }
        )
    }
}

