import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.argm.minipos.data.model.Product
import com.argm.minipos.ui.viewmodel.CartItem
import java.math.BigDecimal

@Composable
fun CartSection(
    modifier: Modifier = Modifier,
    cartItems: List<CartItem>,
    cartTotal: BigDecimal,
    onRemoveItem: (CartItem) -> Unit,
    onUpdateQuantity: (Product, Int) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(start = 8.dp, end = 8.dp, top = 0.dp, bottom = 8.dp)
    ) {
        Text(
            "Carrito Actual",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        if (cartItems.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text("El carrito está vacío.")
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(cartItems, key = { it.product.id }) { cartItem ->
                    CartItemRow(
                        cartItem = cartItem,
                        onRemove = { onRemoveItem(cartItem) },
                        onIncreaseQuantity = { onUpdateQuantity(cartItem.product, cartItem.quantity + 1) },
                        onDecreaseQuantity = { onUpdateQuantity(cartItem.product, cartItem.quantity - 1) }
                    )
                    Divider()
                }
            }
        }
    }
}