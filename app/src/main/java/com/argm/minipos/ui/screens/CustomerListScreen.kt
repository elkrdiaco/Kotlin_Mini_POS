package com.argm.minipos.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.argm.minipos.data.model.Customer
import com.argm.minipos.ui.viewmodel.CustomerViewModel
import com.argm.minipos.utils.UiResult
import java.text.NumberFormat

val SELECTED_CUSTOMER_RUT_KEY = "selected_customer_rut"

@Composable
fun CustomerListScreen(
    navController: NavController,
    viewModel: CustomerViewModel = hiltViewModel()
) {
    val customersUiState by viewModel.customers.collectAsState()
    var showAddCustomerDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddCustomerDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add Customer")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize()) {

            when (val result = customersUiState) {
                is UiResult.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is UiResult.Success -> {
                    val customers = result.data
                    if (customers.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No customers found. Click the '+' button to add one.")
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(customers) { customer ->
                                CustomerItem(
                                    customer = customer,
                                    onClick = {
                                    navController.previousBackStackEntry
                                        ?.savedStateHandle
                                        ?.set(SELECTED_CUSTOMER_RUT_KEY, customer.rut)
                                    navController.popBackStack()
                                })
                                HorizontalDivider()
                            }
                        }
                    }
                }
                is UiResult.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Error: ${result.message}")
                    }
                }
            }
        }
    }

    if (showAddCustomerDialog) {
        AddCustomerDialog(
            viewModel = viewModel,
            onDismiss = { showAddCustomerDialog = false }
        )
    }
}

@Composable
fun CustomerItem(customer: Customer, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick,
                interactionSource = interactionSource,
                indication = rememberRipple(bounded = true)
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(customer.rut, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(customer.name ?: "Sin nombre", fontSize = 14.sp)
        }
        val currencyFormat = NumberFormat.getCurrencyInstance(java.util.Locale("es", "CL"))
        Text(
            currencyFormat.format(customer.balance),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomerDialog(
    viewModel: CustomerViewModel,
    onDismiss: () -> Unit
) {
    var rut by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    val addCustomerResult by viewModel.addCustomerResult.collectAsState()
    DisposableEffect(Unit) {
        onDispose {
            if (addCustomerResult !is UiResult.Loading) {
                 viewModel.clearAddCustomerResult()
            }
        }
    }


    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Customer") },
        text = {
            Column {
                OutlinedTextField(
                    value = rut,
                    onValueChange = { rut = it },
                    label = { Text("Customer RUT") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Customer Name (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                when (val result = addCustomerResult) {
                    is UiResult.Loading -> {
                    }
                    is UiResult.Success -> {
                        if (result.data != null) {
                            LaunchedEffect(result.data) {
                                onDismiss()
                                viewModel.clearAddCustomerResult()
                            }
                        }
                    }
                    is UiResult.Error -> {
                        Text("Error: ${result.message}", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                viewModel.addCustomer(rut, name.ifBlank { null })
            }) {
                Text("Add")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
