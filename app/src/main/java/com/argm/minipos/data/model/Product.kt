package com.argm.minipos.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import java.util.UUID

@Entity(tableName = "products")
data class Product(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String? = null,
    val price: Double,
    val sku: String? = null,
    val category: String? = null,
    val stockQuantity: Int = 0,
    val imageUrl: String? = null
) : Flow<Product?> {
    override suspend fun collect(collector: FlowCollector<Product?>) {
        collector.emit(this)
    }

    init {
        require(name.isNotBlank()) { "El nombre del producto no puede estar vacío." }
        require(price >= 0) { "El precio del producto no puede ser negativo." }
        require(stockQuantity >= 0) { "La cantidad en stock no puede ser negativa." }
    }
}
