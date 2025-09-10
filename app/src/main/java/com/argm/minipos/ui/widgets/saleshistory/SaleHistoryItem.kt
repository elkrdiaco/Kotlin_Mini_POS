package com.argm.minipos.ui.widgets.saleshistory

import androidx.compose.foundation.background // Nuevo import
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box // Nuevo import
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size // Nuevo import
import androidx.compose.foundation.shape.RoundedCornerShape // Nuevo import
import androidx.compose.material.icons.Icons // Nuevo import
import androidx.compose.material.icons.outlined.Schedule // Nuevo import (o el ícono que prefieras)
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon // Nuevo import
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip // Nuevo import
import androidx.compose.ui.graphics.Color // Nuevo import (opcional, para el color de fondo)
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.argm.minipos.data.model.SaleWithItems
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun SaleHistoryItem(saleWithItems: SaleWithItems) {
    val dateFormatter = remember {
        SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Venta #${saleWithItems.sale.id.toString().take(8)}...",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp)) // Esquinas redondeadas
                        .background(MaterialTheme.colorScheme.surfaceContainer) // Fondo con transparencia
                        .padding(horizontal = 8.dp, vertical = 4.dp) // Padding interno
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = "Fecha de venta",
                            modifier = Modifier.size(16.dp), // Ajusta el tamaño del ícono
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.size(4.dp)) // Espacio entre ícono y texto
                        Text(
                            dateFormatter.format(saleWithItems.sale.timestamp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                // --- Fin del cambio visual para la fecha ---
            }

            if (saleWithItems.items.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Producto(s):",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    saleWithItems.items.forEach { item ->
                        val itemTotalPrice = item.priceAtSale * item.quantity.toBigDecimal()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "• ${item.productName} (x${item.quantity})",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "$${"%.2f".format(itemTotalPrice)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Total Producto(s): ${saleWithItems.items.sumOf { it.quantity }}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Text(
                "Total General: $${"%.2f".format(saleWithItems.sale.totalAmount)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

