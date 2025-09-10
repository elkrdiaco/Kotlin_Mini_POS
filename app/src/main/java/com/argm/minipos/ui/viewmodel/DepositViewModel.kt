package com.argm.minipos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.argm.minipos.data.repository.CustomerRepository
import com.argm.minipos.data.repository.PendingOperationRepository
import com.argm.minipos.util.UiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject
import kotlin.random.Random

// DepositUiState, DepositService, SimulatedDepositService (sin cambios respecto a su definición original que ya tenías)
// Asegúrate que DepositUiState.depositSuccess se interprete como:
// "la operación de depósito se ha completado Y el saldo local está actualizado" O "la operación se ha guardado exitosamente como pendiente".
data class DepositUiState(
    val selectedCustomerRut: String? = null,
    val selectedCustomerName: String? = null,
    val amount: String = "",
    val isLoading: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false,
    val depositSuccess: Boolean = false, // True if operation is completed OR successfully saved as pending
    val isOnline: Boolean = true
)

interface DepositService {
    suspend fun makeDeposit(amount: Double, isOnline: Boolean): Result<String> // isOnline aquí puede ser un hint para el servicio
}

class SimulatedDepositService @Inject constructor() : DepositService {
    override suspend fun makeDeposit(amount: Double, isOnline: Boolean): Result<String> {
        delay(Random.nextLong(600, 1200))
        // El servicio puede tener su propia lógica de fallo incluso si el ViewModel piensa que está online
        if (Random.nextFloat() < 0.2) { // 20% chance de fallo simulado del servidor
            return Result.failure(Exception("Error simulado por el servicio de depósito"))
        }
        val commission = amount * 0.01 // Ejemplo de lógica de servicio
        val netAmount = amount - commission
        val formatter = NumberFormat.getCurrencyInstance(Locale("es", "CL"))
        return Result.success("Depósito de ${formatter.format(amount)} procesado por servidor. Neto: ${formatter.format(netAmount)}")
    }
}

@HiltViewModel
class DepositViewModel @Inject constructor(
    private val customerRepository: CustomerRepository,
    private val depositService: DepositService,
    private val pendingOperationRepository: PendingOperationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DepositUiState())
    val uiState: StateFlow<DepositUiState> = _uiState.asStateFlow()

    // onAmountChange, onCustomerSelected, clearCustomerSelection, onOnlineStatusChange (sin cambios)
    fun onAmountChange(newAmount: String) {
        val validAmount = newAmount.filter { it.isDigit() || it == '.' }
            .let { current ->
                val parts = current.split('.')
                if (parts.size > 1) {
                    parts[0] + "." + parts.subList(1, parts.size).joinToString("").take(2)
                } else {
                    current
                }
            }
        _uiState.update { currentState ->
            currentState.copy(
                amount = validAmount,
                message = null,
                isError = false,
                depositSuccess = false // Resetear en cambio de monto
            )
        }
    }

    fun onCustomerSelected(rut: String) {
        _uiState.update {
            it.copy(
                selectedCustomerRut = rut,
                selectedCustomerName = "Cargando...",
                message = null,
                isError = false,
                depositSuccess = false // Resetear en cambio de cliente
            )
        }
        customerRepository.getCustomerByRut(rut).onEach { customer ->
            _uiState.update {
                it.copy(selectedCustomerName = customer?.name ?: "No encontrado")
            }
        }.launchIn(viewModelScope)
    }

    fun clearCustomerSelection() {
         _uiState.update {
            it.copy(
                selectedCustomerRut = null,
                selectedCustomerName = null,
                message = null,
                isError = false,
                depositSuccess = false
            )
        }
    }

    fun onOnlineStatusChange(isOnline: Boolean) {
        _uiState.update { it.copy(isOnline = isOnline, message = null, isError = false, depositSuccess = false) }
    }


    fun performDeposit() {
        val currentState = _uiState.value
        val rut = currentState.selectedCustomerRut
        val amountString = currentState.amount

        // Validaciones
        if (rut == null) {
            _uiState.update { it.copy(isLoading = false, message = "Seleccione un cliente.", isError = true, depositSuccess = false) }
            return
        }
        if (amountString.isBlank()) {
            _uiState.update { it.copy(isLoading = false, message = "El monto no puede estar vacío.", isError = true, depositSuccess = false) }
            return
        }
        val amountValue = amountString.toDoubleOrNull()
        if (amountValue == null || amountValue <= 0) {
            _uiState.update { it.copy(isLoading = false, message = "Ingrese un monto válido.", isError = true, depositSuccess = false) }
            return
        }

        _uiState.update { it.copy(isLoading = true, message = null, isError = false, depositSuccess = false) }

        viewModelScope.launch {
            val customerName = currentState.selectedCustomerName ?: rut
            val formattedAmount = NumberFormat.getCurrencyInstance(Locale("es", "CL")).format(amountValue)
            // Usaremos un tipo de operación pendiente unificado que indique que el saldo local AÚN NO se ha actualizado.
            val pendingOperationType = "DEPOSIT_AWAITING_SYNC_AND_BALANCE_UPDATE"
            val operationData = mapOf(
                "rut" to rut,
                "amount" to amountValue,
                "customerName" to customerName // Para mensajes más claros en SyncScreen si es necesario
            )

            if (currentState.isOnline) {
                val serverResult = depositService.makeDeposit(amountValue, true)
                if (serverResult.isSuccess) {
                    // Sincronización con servidor exitosa, AHORA actualizar saldo local
                    val localResult = customerRepository.addBalanceToCustomer(rut, amountValue)
                    when (localResult) {
                        is UiResult.Success -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    message = "Depósito de $formattedAmount para $customerName completado y sincronizado. ${serverResult.getOrNull()}",
                                    isError = false,
                                    depositSuccess = true, // Éxito completo
                                    amount = "" // Limpiar monto
                                )
                            }
                        }
                        is UiResult.Error -> {
                            pendingOperationRepository.addOperation(
                                type = "DEPOSIT_SERVER_OK_LOCAL_BALANCE_FAILED",
                                data = operationData + mapOf("serverMessage" to (serverResult.getOrNull() ?: ""), "localError" to (localResult.message ?: "Error desconocido"))
                            )
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    message = "Sincronizado con servidor, pero falló la actualización local: ${localResult.message}. Registrado como pendiente para revisión.",
                                    isError = true, // Indica un problema
                                    depositSuccess = false // No fue un éxito completo en el flujo deseado
                                )
                            }
                        }
                        else -> { /* No aplicable para Loading en este contexto */ }
                    }
                } else {
                    // Sincronización con servidor falló: Guardar como pendiente SIN actualizar saldo local
                    pendingOperationRepository.addOperation(
                        type = pendingOperationType, // Reutilizamos el tipo general
                        data = operationData + mapOf("serverError" to (serverResult.exceptionOrNull()?.message ?: "Error desconocido del servidor"))
                    )
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            message = "Falló la sincronización con el servidor. Depósito de $formattedAmount para $customerName guardado como pendiente. Error: ${serverResult.exceptionOrNull()?.message}",
                            isError = true, // Hubo un error de sincronización
                            depositSuccess = true // PERO se guardó como pendiente exitosamente
                        )
                    }
                }
            } else {
                // Modo Offline: Guardar como pendiente SIN actualizar saldo local
                pendingOperationRepository.addOperation(
                    type = pendingOperationType,
                    data = operationData
                )
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        message = "Estás offline. Depósito de $formattedAmount para $customerName guardado como pendiente.",
                        isError = false, // No es un error, es comportamiento offline esperado
                        depositSuccess = true // Se guardó como pendiente exitosamente
                    )
                }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null, isError = false) }
    }
}
