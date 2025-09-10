package com.argm.minipos.ui.deposit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun DepositScreen(depositViewModel: DepositViewModel = hiltViewModel()) {
    val uiState by depositViewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        OutlinedTextField(
            value = uiState.amount,
            onValueChange = { depositViewModel.onAmountChange(it) },
            label = { Text("Monto del depósito") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            isError = uiState.amountError != null,
            supportingText = {
                uiState.amountError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Simular Conexión Online")
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = uiState.isOnline,
                onCheckedChange = { depositViewModel.onOnlineStatusChange(it) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { depositViewModel.performDeposit() },
            enabled = !uiState.isLoading
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.height(24.dp))
            } else {
                Text("Depositar")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        uiState.operationResult?.let {
            Text(it, color = if (uiState.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DepositScreenPreview() {
    // Preview con un ViewModel de Hilt puede requerir configuración adicional
    // o un ViewModel falso para una previsualización aislada.
    DepositScreen()
}
