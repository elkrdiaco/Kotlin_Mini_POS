package com.argm.minipos.data.repository

import android.database.sqlite.SQLiteConstraintException
import android.util.Log
import androidx.room.withTransaction
import com.argm.minipos.data.local.AppDatabase
import com.argm.minipos.data.local.dao.ProductDao
import com.argm.minipos.data.local.dao.SaleDao
import com.argm.minipos.data.model.Sale
import com.argm.minipos.data.model.SaleItem
import com.argm.minipos.data.model.SaleWithItems
import com.argm.minipos.util.UiResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class StockInsufficientException(message: String) : Exception(message)

class SaleRepositoryImpl @Inject constructor(
    private val saleDao: SaleDao,
    private val productDao: ProductDao,
    private val appDatabase: AppDatabase
) : SaleRepository {

    override suspend fun finalizeSale(sale: Sale, items: List<SaleItem>): UiResult<String> {
        return withContext(Dispatchers.IO) {
            var generatedSaleId: Long = -1L

            try {
                appDatabase.withTransaction {
                    generatedSaleId = saleDao.insertSale(sale)

                    if (generatedSaleId <= 0) {
                        throw Exception("Error al insertar la venta, ID no generado.")
                    }

                    val itemsWithCorrectSaleId = items.map {
                        it.copy(saleId = generatedSaleId)
                    }

                    saleDao.insertSaleItems(itemsWithCorrectSaleId)

                    itemsWithCorrectSaleId.forEach { saleItem ->
                        val currentStock = productDao.getProductStock(saleItem.productId)
                            ?: throw StockInsufficientException("Producto ID: ${saleItem.productId} no encontrado o stock es null.")

                        if (currentStock < saleItem.quantity) {
                            throw StockInsufficientException(
                                "Stock insuficiente para producto ID: ${saleItem.productId}. " +
                                        "Disponible: $currentStock, Requerido: ${saleItem.quantity}"
                            )
                        }
                        productDao.decreaseStock(saleItem.productId, saleItem.quantity)
                    }
                }
                UiResult.Success(generatedSaleId.toString())
            } catch (e: StockInsufficientException) {
                Log.e("SaleRepositoryImpl", "Error de stock al finalizar venta: ${e.message}")
                UiResult.Error(e.message ?: "Error de stock desconocido.")
            } catch (e: SQLiteConstraintException) {
                Log.e("SaleRepositoryImpl", "Error de constraint SQLite (FK?): ${e.message}", e)
                UiResult.Error("Error de datos al guardar la venta. Verifique los productos.")
            } catch (e: Exception) {
                Log.e("SaleRepositoryImpl", "Error genérico al finalizar venta: ${e.message}", e)
                UiResult.Error("Ocurrió un error al procesar la venta: ${e.message}")
            }
        }
    }

    override fun getSalesHistory(): Flow<List<SaleWithItems>> {
        return saleDao.getAllSalesWithItems()
    }
}
