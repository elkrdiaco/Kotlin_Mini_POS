package com.argm.minipos.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.argm.minipos.data.local.dao.CustomerDao
import com.argm.minipos.data.local.dao.PendingDepositDao
import com.argm.minipos.data.local.dao.ProductDao
import com.argm.minipos.data.local.dao.SaleDao
import com.argm.minipos.data.model.Customer
import com.argm.minipos.data.model.PendingDeposit
import com.argm.minipos.data.model.Product
import com.argm.minipos.data.model.Sale
import com.argm.minipos.data.model.SaleItem
import com.argm.minipos.util.Converters

@Database(
    entities = [
        Product::class,
        Sale::class,
        SaleItem::class,
        Customer::class,
        PendingDeposit::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun saleDao(): SaleDao
    abstract fun customerDao(): CustomerDao
    abstract fun pendingDepositDao(): PendingDepositDao
}
