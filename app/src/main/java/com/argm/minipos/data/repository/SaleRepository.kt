package com.argm.minipos.data.repository

import com.argm.minipos.data.model.Sale
import com.argm.minipos.data.model.SaleItem
import com.argm.minipos.data.model.SaleWithItems
import com.argm.minipos.util.UiResult
import kotlinx.coroutines.flow.Flow

interface SaleRepository {
    suspend fun finalizeSale(sale: Sale, items: List<SaleItem>): UiResult<String>

    fun getSalesHistory(): Flow<List<SaleWithItems>>
}
