package com.argm.minipos.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

// Data class for a pending operation.
// You might already have a similar one in SyncScreen.kt.
// Ensure consistency or centralize this definition.
// For this example, it's defined here.
data class PendingOperation(
    val id: String = UUID.randomUUID().toString(),
    val type: String, // e.g., "DEPOSIT"
    val data: Map<String, Any>, // Flexible data for the operation (e.g., amount, accountId)
    val timestamp: Long = System.currentTimeMillis(),
    var attempts: Int = 0,
    var lastAttemptTimestamp: Long? = null,
    var status: String = "PENDING" // PENDING, FAILED_RETRY, PROCESSING
)

interface PendingOperationRepository {
    suspend fun addOperation(type: String, data: Map<String, Any>)
    fun getPendingOperations(): Flow<List<PendingOperation>>
    suspend fun updateOperation(operation: PendingOperation)
    suspend fun deleteOperations(operationIds: List<String>)
    suspend fun getOperation(id: String): PendingOperation?
    suspend fun clearAllOperations() // For testing or specific scenarios
}

@Singleton
class InMemoryPendingOperationRepository @Inject constructor() : PendingOperationRepository {

    private val _pendingOperations = MutableStateFlow<List<PendingOperation>>(emptyList())

    override suspend fun addOperation(type: String, data: Map<String, Any>) {
        val newOperation = PendingOperation(type = type, data = data)
        _pendingOperations.update { currentList ->
            currentList + newOperation
        }
        println("Repository: Added operation ${newOperation.id}. Total: ${_pendingOperations.value.size}")
    }

    override fun getPendingOperations(): Flow<List<PendingOperation>> {
        println("Repository: Getting operations stream. Current count: ${_pendingOperations.value.size}")
        return _pendingOperations.asStateFlow()
    }

    override suspend fun updateOperation(operation: PendingOperation) {
        _pendingOperations.update { currentList ->
            currentList.map {
                if (it.id == operation.id) operation else it
            }
        }
        println("Repository: Updated operation ${operation.id}.")
    }

    override suspend fun deleteOperations(operationIds: List<String>) {
        _pendingOperations.update { currentList ->
            currentList.filterNot { operationIds.contains(it.id) }
        }
        println("Repository: Deleted operations ${operationIds.joinToString()}. Remaining: ${_pendingOperations.value.size}")
    }

    override suspend fun getOperation(id: String): PendingOperation? {
        return _pendingOperations.value.find { it.id == id }
    }

    override suspend fun clearAllOperations() {
        _pendingOperations.value = emptyList()
        println("Repository: Cleared all operations.")
    }
}

