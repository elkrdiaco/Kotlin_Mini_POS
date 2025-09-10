package com.argm.minipos.ui.sync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.argm.minipos.data.repository.PendingOperation // Importa la clase correcta
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// PendingOperation data class is now expected from com.argm.minipos.data.repository
// If it was also defined locally in this file previously, that local definition should be removed.

@Composable
fun SyncScreen(
    syncViewModel: SyncViewModel = hiltViewModel()
) {
    val uiState by syncViewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Operaciones Pendientes", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.isLoading) {
            CircularProgressIndicator()
        } else {
            if (uiState.pendingOperations.isEmpty()) {
                Text("No hay operaciones pendientes para sincronizar.")
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(uiState.pendingOperations, key = { it.id }) { operation ->
                        PendingOperationItem(operation)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                syncViewModel.synchronizePendingOperations()
            },
            enabled = !uiState.isLoading && uiState.pendingOperations.isNotEmpty()
        ) {
            Text("Sincronizar Todo")
        }

        uiState.syncStatusMessage?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = if (uiState.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun PendingOperationItem(operation: PendingOperation) {
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("ID: ${operation.id}", style = MaterialTheme.typography.labelSmall)
            Text("Tipo: ${operation.type.uppercase()}", style = MaterialTheme.typography.titleSmall)
            Text("Data: ${operation.data.map { "${it.key}: ${it.value}" }.joinToString()}", style = MaterialTheme.typography.bodyMedium)
            Text(
                "Registrado: ${dateFormatter.format(Date(operation.timestamp))}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "Estado: ${operation.status}", // Displaying status
                style = MaterialTheme.typography.bodySmall
            )
            if (operation.attempts > 0) {
                Text(
                    "Reintentos: ${operation.attempts}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            operation.lastAttemptTimestamp?.let {
                Text(
                    "Último intento: ${dateFormatter.format(Date(it))}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Preview(showBackground = true, device = "spec:width=360dp,height=640dp,dpi=480")
@Composable
fun SyncScreenPreview() {
    MaterialTheme {
        // Previewing SyncScreen might be tricky if it directly relies on a Hilt ViewModel.
        // For a more robust preview, you might pass a fake UiState or a fake ViewModel.
        // For now, this will use the default constructor of SyncViewModel if Hilt is not fully active in preview.
        SyncScreen(
            // You can create a fake SyncViewModel for preview if needed:
            // syncViewModel = FakeSyncViewModel()
        )
    }
}
