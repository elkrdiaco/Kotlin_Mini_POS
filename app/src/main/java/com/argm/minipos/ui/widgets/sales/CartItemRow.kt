import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.argm.minipos.ui.viewmodel.CartItem
import com.argm.minipos.utils.formatPrice

@Composable
fun CartItemRow(
    cartItem: CartItem,
    onRemove: () -> Unit,
    onIncreaseQuantity: () -> Unit,
    onDecreaseQuantity: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(cartItem.product.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text("Cant: ${cartItem.quantity} x $${formatPrice(cartItem.product.price)}", style = MaterialTheme.typography.bodyMedium)
                Text("Subtotal: $${formatPrice(cartItem.subtotal.toDouble())}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDecreaseQuantity, Modifier.size(36.dp)) {
                    Icon(Icons.Filled.RemoveCircle, contentDescription = "Disminuir cantidad", tint = MaterialTheme.colorScheme.primary)
                }
                Text("${cartItem.quantity}", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(horizontal = 4.dp))
                IconButton(onClick = onIncreaseQuantity, Modifier.size(36.dp)) {
                    Icon(Icons.Filled.AddCircle, contentDescription = "Aumentar cantidad", tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = onRemove, Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Delete, contentDescription = "Eliminar del carrito", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
