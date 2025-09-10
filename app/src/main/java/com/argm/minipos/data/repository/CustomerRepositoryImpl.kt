package com.argm.minipos.data.repository

import androidx.room.withTransaction
import com.argm.minipos.data.local.AppDatabase
import com.argm.minipos.data.local.dao.CustomerDao
import com.argm.minipos.data.local.dao.PendingDepositDao
import com.argm.minipos.data.model.Customer
import com.argm.minipos.data.model.PendingDeposit
import com.argm.minipos.util.UiResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CustomerRepositoryImpl @Inject constructor(
    private val customerDao: CustomerDao,
    private val pendingDepositDao: PendingDepositDao,
    private val appDatabase: AppDatabase
) : CustomerRepository {

    override suspend fun addCustomer(customer: Customer): UiResult<Customer> = withContext(Dispatchers.IO) {
        try {
            val existingCustomer = customerDao.getCustomerByRutOnce(customer.rut)
            if (existingCustomer != null) {
                return@withContext UiResult.Error("Cliente con RUT ${customer.rut} ya existe.")
            }
            customerDao.insertCustomer(customer)
            UiResult.Success(customer)
        } catch (e: Exception) {
            UiResult.Error("Error al registrar cliente: ${e.message}")
        }
    }

    override fun getCustomerByRut(rut: String): Flow<Customer?> {
        return customerDao.getCustomerByRut(rut).flowOn(Dispatchers.IO)
    }

    override fun getAllCustomers(): Flow<List<Customer>> {
        return customerDao.getAllCustomers().flowOn(Dispatchers.IO)
    }

    override suspend fun updateCustomerBalance(rut: String, newBalance: Double): UiResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val customer = customerDao.getCustomerByRutOnce(rut)
            if (customer == null) {
                UiResult.Error("Cliente con RUT $rut no encontrado.")
            } else {
                customerDao.updateCustomer(customer.copy(balance = newBalance))
                UiResult.Success(Unit)
            }
        } catch (e: Exception) {
            UiResult.Error("Error al actualizar saldo: ${e.message}")
        }
    }

    override suspend fun syncPendingDepositsForCustomer(rut: String): UiResult<String> = withContext(Dispatchers.IO) {
        try {
            val customer = customerDao.getCustomerByRutOnce(rut)
            if (customer == null) {
                return@withContext UiResult.Error("Cliente con RUT $rut no encontrado para sincronizar.")
            }

            val pendingDeposits = pendingDepositDao.getPendingDepositsForCustomerOnce(rut)
            if (pendingDeposits.isEmpty()) {
                return@withContext UiResult.Success("No hay depósitos pendientes para sincronizar para el RUT $rut.")
            }

            appDatabase.withTransaction {
                var currentBalance = customer.balance
                pendingDeposits.forEach { deposit ->
                    currentBalance += deposit.amount
                }
                customerDao.updateCustomer(customer.copy(balance = currentBalance))
                val idsToDelete = pendingDeposits.map { it.id }
                pendingDepositDao.deleteDepositsByIds(idsToDelete)
            }
            UiResult.Success("Depósitos sincronizados correctamente para el RUT $rut.")
        } catch (e: Exception) {
            UiResult.Error("Error al sincronizar depósitos para RUT $rut: ${e.message}")
        }
    }

    override suspend fun recordDeposit(rut: String, amount: Double, isOffline: Boolean): UiResult<Unit> = withContext(Dispatchers.IO) {
        if (amount <= 0) {
            return@withContext UiResult.Error("El monto del depósito debe ser positivo.")
        }
        try {
            if (isOffline) {
                val pendingDeposit = PendingDeposit(
                    customerRut = rut,
                    amount = amount,
                    timestamp = System.currentTimeMillis()
                )
                pendingDepositDao.insert(pendingDeposit)
                UiResult.Success(Unit)
            } else {
                appDatabase.withTransaction {
                    val customer = customerDao.getCustomerByRutOnce(rut)
                    if (customer == null) {
                        throw Exception("Cliente con RUT $rut no encontrado.")
                    }
                    val newBalance = customer.balance + amount
                    customerDao.updateCustomer(customer.copy(balance = newBalance))
                }
                UiResult.Success(Unit)
            }
        } catch (e: Exception) {
            UiResult.Error("Error al registrar depósito para RUT $rut: ${e.message}")
        }
    }

    override suspend fun addBalanceToCustomer(rut: String, amountToAdd: Double): UiResult<Unit> = withContext(Dispatchers.IO) {
        if (amountToAdd <= 0) {
            return@withContext UiResult.Error("El monto a añadir debe ser positivo.")
        }
        try {
            val customer = customerDao.getCustomerByRutOnce(rut)
            if (customer == null) {
                UiResult.Error("Cliente con RUT $rut no encontrado.")
            } else {
                val newBalance = customer.balance + amountToAdd
                customerDao.updateCustomer(customer.copy(balance = newBalance))
                UiResult.Success(Unit)
            }
        } catch (e: Exception) {
            UiResult.Error("Error al añadir saldo al cliente: ${e.message}")
        }
    }

    override suspend fun deductBalanceFromCustomer(rut: String, amountToDeduct: Double): UiResult<Unit> = withContext(Dispatchers.IO) {
        if (amountToDeduct < 0) {
            return@withContext UiResult.Error("El monto a descontar no puede ser negativo.")
        }
        if (amountToDeduct == 0.0) {
            return@withContext UiResult.Success(Unit)
        }

        try {
            val customer = customerDao.getCustomerByRutOnce(rut)

            if (customer == null) {
                UiResult.Error("Cliente con RUT $rut no encontrado para descontar saldo.")
            } else {
                val newBalance = customer.balance - amountToDeduct
                customerDao.updateCustomer(customer.copy(balance = newBalance))
                UiResult.Success(Unit)
            }
        } catch (e: Exception) {
            UiResult.Error("Error en base de datos al descontar saldo para RUT $rut: ${e.message}")
        }
    }
}

