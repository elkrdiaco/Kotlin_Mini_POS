package com.argm.minipos.ui.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.argm.minipos.data.repository.PendingOperation
import com.argm.minipos.data.repository.PendingOperationRepository
import com.argm.minipos.ui.deposit.DepositService // Assuming DepositService is in this package
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SyncUiState(
    val pendingOperations: List<PendingOperation> = emptyList(),
    val isLoading: Boolean = false,
    val syncStatusMessage: String? = null,
    val isError: Boolean = false
)

@HiltViewModel
class SyncViewModel @Inject constructor(
    private val pendingOperationRepository: PendingOperationRepository,
    private val depositService: DepositService // To re-process deposits
    // Add other services if you have other types of pending operations
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    private val _syncStatusMessage = MutableStateFlow<String?>(null)
    private val _isError = MutableStateFlow(false)

    val uiState: StateFlow<SyncUiState> = combine(
        pendingOperationRepository.getPendingOperations(),
        _isLoading,
        _syncStatusMessage,
        _isError
    ) { operations, loading, message, error ->
        SyncUiState(
            pendingOperations = operations,
            isLoading = loading,
            syncStatusMessage = message,
            isError = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SyncUiState()
    )

    fun synchronizePendingOperations() {
        viewModelScope.launch {
            _isLoading.value = true
            _syncStatusMessage.value = null
            _isError.value = false
            var operationsProcessed = 0
            var operationsFailed = 0

            val operationsToSync = uiState.value.pendingOperations.toList() // Work on a copy

            if (operationsToSync.isEmpty()) {
                _syncStatusMessage.value = "No hay operaciones para sincronizar."
                _isLoading.value = false
                return@launch
            }

            operationsToSync.forEach { operation ->
                // Mark as processing
                pendingOperationRepository.updateOperation(operation.copy(status = "PROCESSING", lastAttemptTimestamp = System.currentTimeMillis()))

                var success = false
                try {
                    when (operation.type) {
                        "DEPOSIT" -> {
                            val amount = operation.data["amount"] as? Double
                            // We assume 'isOnline' is true for sync attempts.
                            // The original 'isOnline' status at time of creation might be in operation.data if needed.
                            if (amount != null) {
                                val result = depositService.makeDeposit(amount, isOnline = true)
                                if (result.isSuccess) {
                                    pendingOperationRepository.deleteOperations(listOf(operation.id))
                                    operationsProcessed++
                                    success = true
                                } else {
                                    operationsFailed++
                                    val newAttempts = operation.attempts + 1
                                    pendingOperationRepository.updateOperation(
                                        operation.copy(
                                            attempts = newAttempts,
                                            status = if (newAttempts >= MAX_SYNC_ATTEMPTS) "FAILED_PERMANENTLY" else "FAILED_RETRY",
                                            lastAttemptTimestamp = System.currentTimeMillis()
                                        )
                                    )
                                }
                            } else {
                                operationsFailed++ // Invalid data for deposit
                                pendingOperationRepository.updateOperation(operation.copy(status = "FAILED_INVALID_DATA"))
                            }
                        }
                        // Add cases for other operation types here
                        else -> {
                            // Unknown operation type, mark as failed or log
                            operationsFailed++
                            pendingOperationRepository.updateOperation(operation.copy(status = "FAILED_UNKNOWN_TYPE"))
                        }
                    }
                } catch (e: Exception) {
                    operationsFailed++
                    val newAttempts = operation.attempts + 1
                    pendingOperationRepository.updateOperation(
                        operation.copy(
                            attempts = newAttempts,
                            status = if (newAttempts >= MAX_SYNC_ATTEMPTS) "FAILED_PERMANENTLY" else "FAILED_RETRY",
                            lastAttemptTimestamp = System.currentTimeMillis()
                        )
                    )
                }
            }

            _isLoading.value = false
            _syncStatusMessage.value = when {
                operationsProcessed > 0 && operationsFailed == 0 -> "Sincronización completada. $operationsProcessed operaciones procesadas."
                operationsProcessed > 0 && operationsFailed > 0 -> "Sincronización parcial. $operationsProcessed procesadas, $operationsFailed fallaron."
                operationsProcessed == 0 && operationsFailed > 0 -> "Falló la sincronización de $operationsFailed operaciones."
                else -> "No se procesaron operaciones (puede que la lista estuviera vacía o ya procesada)."
            }
            _isError.value = operationsFailed > 0
        }
    }

    companion object {
        const val MAX_SYNC_ATTEMPTS = 3 // Max retries for an operation
    }
}
