import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun SaleSuccessDialog(
    successMessage: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss, // Permite cerrar el diálogo tocando fuera (opcional)
        icon = { // El ícono se puede poner aquí para que esté alineado con el título
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Venta Exitosa",
                modifier = Modifier.size(48.dp), // Un poco más grande para el diálogo
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "¡Éxito!",
                modifier = Modifier.fillMaxWidth(), // Para que el textAlign funcione bien
                textAlign = TextAlign.Center // Centra el título
            )
        },
        text = {
            Text(
                text = successMessage,
                modifier = Modifier.fillMaxWidth(), // Para que el textAlign funcione bien
                textAlign = TextAlign.Center // Centra el mensaje
            )
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center // Centra el botón
            ) {
                Button(onClick = onDismiss) {
                    Text("Aceptar")
                }
            }
        }
    )
}