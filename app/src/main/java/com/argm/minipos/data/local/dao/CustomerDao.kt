package com.argm.minipos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.argm.minipos.data.model.Customer
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer)

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Query("SELECT * FROM customers WHERE rut = :rut")
    fun getCustomerByRut(rut: String): Flow<Customer?>

    @Query("SELECT * FROM customers WHERE rut = :rut")
    suspend fun getCustomerByRutOnce(rut: String): Customer?

    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<Customer>>

    @Query("UPDATE customers SET balance = :newBalance WHERE rut = :rut")
    suspend fun updateBalance(rut: String, newBalance: Double)
}
