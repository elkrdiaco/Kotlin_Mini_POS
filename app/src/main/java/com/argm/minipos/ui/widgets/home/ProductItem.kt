package com.argm.minipos.ui.widgets.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.argm.minipos.data.model.Product
import com.argm.minipos.utils.formatPrice

@Composable
fun ProductItem(product: Product) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = product.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = product.category ?: "Sin categoría",
                style = MaterialTheme.typography.bodySmall
            )
        }
        Text(
            text = "$${formatPrice(product.price)}",
            style = MaterialTheme.typography.titleMedium
        )
    }
}
