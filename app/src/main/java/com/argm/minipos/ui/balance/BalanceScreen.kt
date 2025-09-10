package com.argm.minipos.ui.balance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun BalanceScreen(balanceViewModel: BalanceViewModel = viewModel()) {
    val uiState by balanceViewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        OutlinedTextField(
            value = uiState.accountId,
            onValueChange = { balanceViewModel.onAccountIdChange(it) },
            label = { Text("RUT o N° de cuenta") },
            modifier = Modifier.fillMaxWidth(),
            isError = uiState.accountIdError != null,
            supportingText = { 
                uiState.accountIdError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { balanceViewModel.fetchBalance() },
            enabled = !uiState.isLoading
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.height(24.dp))
            } else {
                Text("Consultar Saldo")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        uiState.balance?.let {
            Text("Saldo: $it")
        }

        uiState.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BalanceScreenPreview() {
    // En el Preview, no podemos instanciar directamente el ViewModel
    // que depende de Hilt o de un SavedStateHandle.
    // Para previsualizaciones más complejas, se pueden crear ViewModels falsos.
    // Por ahora, BalanceScreenPreview se queda así, o se puede simplificar
    // pasando un estado predefinido si se refactoriza BalanceScreen para aceptar uiState directamente.
    BalanceScreen() // Esta preview mostrará el estado inicial del ViewModel real.
}
