package com.argm.minipos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.argm.minipos.data.repository.CustomerRepository // Importar CustomerRepository
import com.argm.minipos.data.repository.PendingOperation
import com.argm.minipos.data.repository.PendingOperationRepository
import com.argm.minipos.util.UiResult // Importar UiResult si CustomerRepository lo usa
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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
    private val depositService: DepositService,
    private val customerRepository: CustomerRepository // Añadido CustomerRepository
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
            _syncStatusMessage.value = "Iniciando sincronización..."
            _isError.value = false
            var operationsProcessedSuccessfully = 0
            var operationsFailedOrRetried = 0

            // Corrected filter: uses operation.type for specific types and operation.status for generic retryable states.
            val operationsToSync = uiState.value.pendingOperations.filter { operation ->
                operation.type == "DEPOSIT_AWAITING_SYNC_AND_BALANCE_UPDATE" ||
                        operation.type == "DEPOSIT_SERVER_OK_LOCAL_BALANCE_FAILED" ||
                        (operation.status == "PENDING" || operation.status == "FAILED_RETRY") // Catch general pending/retry
            }.toList()

            if (operationsToSync.isEmpty()) {
                val totalPending = uiState.value.pendingOperations.size
                _syncStatusMessage.value = if (totalPending > 0) "No hay operaciones que requieran acción inmediata (podrían estar ya procesadas o en un estado final)." else "No hay operaciones para sincronizar."
                _isLoading.value = false
                return@launch
            }

            operationsToSync.forEach { operation ->
                // Ensure we don't re-process an operation within the same sync batch if its status changes
                val currentOperationState = pendingOperationRepository.getOperation(operation.id) ?: return@forEach
                if (currentOperationState.status == "PROCESSING" || currentOperationState.status == "SYNCED_AND_BALANCED" || currentOperationState.status.contains("PERMANENTLY")) {
                    return@forEach // Skip if already processing or in a final state
                }

                pendingOperationRepository.updateOperation(currentOperationState.copy(status = "PROCESSING", lastAttemptTimestamp = System.currentTimeMillis()))

                try {
                    when (currentOperationState.type) {
                        "DEPOSIT_AWAITING_SYNC_AND_BALANCE_UPDATE" -> {
                            val rut = currentOperationState.data["rut"] as? String
                            val amount = currentOperationState.data["amount"] as? Double

                            if (rut == null || amount == null) {
                                operationsFailedOrRetried++
                                pendingOperationRepository.updateOperation(currentOperationState.copy(status = "FAILED_INVALID_DATA"))
                            } else {
                                _syncStatusMessage.value = "Sincronizando ID ${currentOperationState.id.take(6)} (Servidor)..."
                                val serverResult = depositService.makeDeposit(amount, isOnline = true)

                                if (serverResult.isSuccess) {
                                    _syncStatusMessage.value = "ID ${currentOperationState.id.take(6)} OK (Servidor). Actualizando local..."
                                    val localResult = customerRepository.addBalanceToCustomer(rut, amount)
                                    when (localResult) {
                                        is UiResult.Success -> {
                                            pendingOperationRepository.deleteOperations(listOf(currentOperationState.id))
                                            operationsProcessedSuccessfully++
                                        }
                                        is UiResult.Error -> {
                                            operationsFailedOrRetried++
                                            val newAttempts = currentOperationState.attempts + 1
                                            pendingOperationRepository.updateOperation(
                                                currentOperationState.copy(
                                                    status = if (newAttempts >= MAX_SYNC_ATTEMPTS) "FAILED_LOCAL_BALANCE_PERMANENTLY" else "FAILED_LOCAL_BALANCE_RETRY",
                                                    attempts = newAttempts,
                                                    lastAttemptTimestamp = System.currentTimeMillis()
                                                )
                                            )
                                        }
                                        else -> {}
                                    }
                                } else { // Fallo del servidor
                                    operationsFailedOrRetried++
                                    val newAttempts = currentOperationState.attempts + 1
                                    pendingOperationRepository.updateOperation(
                                        currentOperationState.copy(
                                            attempts = newAttempts,
                                            status = if (newAttempts >= MAX_SYNC_ATTEMPTS) "FAILED_SERVER_SYNC_PERMANENTLY" else "FAILED_RETRY",
                                            lastAttemptTimestamp = System.currentTimeMillis()
                                        )
                                    )
                                }
                            }
                        }
                        "DEPOSIT_SERVER_OK_LOCAL_BALANCE_FAILED" -> {
                            val rut = currentOperationState.data["rut"] as? String
                            val amount = currentOperationState.data["amount"] as? Double
                            if (rut == null || amount == null) {
                                operationsFailedOrRetried++
                                pendingOperationRepository.updateOperation(currentOperationState.copy(status = "FAILED_INVALID_DATA"))
                            } else {
                                _syncStatusMessage.value = "Reintentando ID ${currentOperationState.id.take(6)} (Local)..."
                                val localResult = customerRepository.addBalanceToCustomer(rut, amount)
                                when (localResult) {
                                    is UiResult.Success -> {
                                        pendingOperationRepository.deleteOperations(listOf(currentOperationState.id))
                                        operationsProcessedSuccessfully++
                                    }
                                    is UiResult.Error -> {
                                        operationsFailedOrRetried++
                                        val newAttempts = currentOperationState.attempts + 1
                                        pendingOperationRepository.updateOperation(
                                            currentOperationState.copy(
                                                status = if (newAttempts >= MAX_SYNC_ATTEMPTS) "FAILED_LOCAL_BALANCE_PERMANENTLY" else "FAILED_LOCAL_BALANCE_RETRY",
                                                attempts = newAttempts,
                                                lastAttemptTimestamp = System.currentTimeMillis()
                                            )
                                        )
                                    }
                                    else -> {}
                                }
                            }
                        }
                        // Consider other types if they exist and follow the "PENDING" or "FAILED_RETRY" status logic
                        else -> {
                            if (currentOperationState.status == "PENDING" || currentOperationState.status == "FAILED_RETRY") {
                                // Generic handling for other types that might just need a server sync
                                // For example, if you had a generic "DEPOSIT" type that implied local balance was already updated
                                // _syncStatusMessage.value = "Procesando tipo genérico ${currentOperationState.type}..."
                                // This section needs specific logic based on what other types mean.
                                // For now, mark as unknown if not one of the specific customer deposit types.
                                operationsFailedOrRetried++
                                pendingOperationRepository.updateOperation(currentOperationState.copy(status = "FAILED_UNKNOWN_TYPE_IN_SYNC"))
                            } else {
                                // This operation was in operationsToSync but its type/status doesn't match known logic.
                                // This should ideally not happen if the initial filter is correct.
                            }
                        }
                    }
                } catch (e: Exception) {
                    operationsFailedOrRetried++
                    val newAttempts = currentOperationState.attempts + 1
                    pendingOperationRepository.updateOperation(
                        currentOperationState.copy(
                            attempts = newAttempts,
                            status = if (newAttempts >= MAX_SYNC_ATTEMPTS) "FAILED_UNEXPECTED_ERROR_PERMANENTLY" else "FAILED_RETRY",
                            lastAttemptTimestamp = System.currentTimeMillis()
                        )
                    )
                }
            }

            _isLoading.value = false
            val finalMessage = when {
                operationsToSync.isEmpty() && uiState.value.pendingOperations.isNotEmpty() -> "No hay operaciones que requieran sincronización en este momento."
                operationsToSync.isEmpty() -> "No hay operaciones para sincronizar."
                operationsProcessedSuccessfully > 0 && operationsFailedOrRetried == 0 -> "Sincronización completada. $operationsProcessedSuccessfully operaciones procesadas."
                operationsProcessedSuccessfully > 0 && operationsFailedOrRetried > 0 -> "Sincronización parcial. $operationsProcessedSuccessfully procesadas, $operationsFailedOrRetried fallaron o requieren reintento."
                operationsProcessedSuccessfully == 0 && operationsFailedOrRetried > 0 -> "Falló la sincronización de $operationsFailedOrRetried operaciones o requieren reintento."
                else -> "No se procesaron nuevas operaciones o todas estaban en un estado final."
            }
            _syncStatusMessage.value = finalMessage
            _isError.value = operationsFailedOrRetried > 0
        }
    }

    companion object {
        const val MAX_SYNC_ATTEMPTS = 3
    }
}
