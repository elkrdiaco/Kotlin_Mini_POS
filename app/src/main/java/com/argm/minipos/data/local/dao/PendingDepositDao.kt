package com.argm.minipos.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.argm.minipos.data.model.PendingDeposit
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingDepositDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pendingDeposit: PendingDeposit): Long

    @Query("SELECT * FROM pending_deposits WHERE customerRut = :customerRut ORDER BY timestamp DESC")
    fun getPendingDepositsForCustomer(customerRut: String): Flow<List<PendingDeposit>>

    @Query("SELECT * FROM pending_deposits WHERE customerRut = :customerRut ORDER BY timestamp ASC")
    suspend fun getPendingDepositsForCustomerOnce(customerRut: String): List<PendingDeposit>

    @Delete
    suspend fun delete(pendingDeposit: PendingDeposit)

    @Query("DELETE FROM pending_deposits WHERE id IN (:ids)")
    suspend fun deleteDepositsByIds(ids: List<Long>)

    @Query("SELECT * FROM pending_deposits ORDER BY timestamp DESC")
    fun getAllPendingDeposits(): Flow<List<PendingDeposit>>
}
