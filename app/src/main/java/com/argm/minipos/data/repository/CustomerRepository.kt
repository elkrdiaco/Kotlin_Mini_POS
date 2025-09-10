package com.argm.minipos.data.repository

import com.argm.minipos.data.model.Customer
import com.argm.minipos.utils.UiResult
import kotlinx.coroutines.flow.Flow

interface CustomerRepository {
    suspend fun addCustomer(customer: Customer): UiResult<Customer>

    fun getCustomerByRut(rut: String): Flow<Customer?>
    fun getAllCustomers(): Flow<List<Customer>>
    suspend fun updateCustomerBalance(rut: String, newBalance: Double): UiResult<Unit>

    suspend fun syncPendingDepositsForCustomer(rut: String): UiResult<String>

    suspend fun recordDeposit(rut: String, amount: Double, isOffline: Boolean): UiResult<Unit>

    suspend fun addBalanceToCustomer(rut: String, amountToAdd: Double): UiResult<Unit>

    suspend fun deductBalanceFromCustomer(rut: String, amountToDeduct: Double): UiResult<Unit>
}