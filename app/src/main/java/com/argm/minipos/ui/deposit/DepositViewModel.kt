package com.argm.minipos.ui.deposit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.argm.minipos.data.repository.PendingOperationRepository // Importa el repositorio
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject
import kotlin.random.Random

// --- Data State ---
data class DepositUiState(
    val amount: String = "",
    val isOnline: Boolean = true,
    val isLoading: Boolean = false,
    val operationResult: String? = null,
    val isError: Boolean = false,
    val amountError: String? = null
)

// --- Service ---
interface DepositService {
    suspend fun makeDeposit(amount: Double, isOnline: Boolean): Result<String>
}

class SimulatedDepositService @Inject constructor() : DepositService {
    override suspend fun makeDeposit(amount: Double, isOnline: Boolean): Result<String> {
        delay(Random.nextLong(600, 1200)) // Simulate network delay

        // La lógica de SimulatedDepositService ya no necesita manejar el caso !isOnline directamente,
        // ya que DepositViewModel lo hará antes de llamar a makeDeposit.
        // Sin embargo, mantener la verificación aquí como una doble seguridad o para otros usos del servicio no está mal.
        if (!isOnline) {
            // Este caso ahora debería ser manejado mayormente por el ViewModel guardando la operación pendiente.
            // Pero si se llama directamente al servicio en modo offline, este sería el comportamiento.
            return Result.failure(Exception("Servicio: Operación fallida por estar offline."))
        }

        return if (Random.nextFloat() < 0.2) { // 20% chance of error
            Result.failure(Exception("Error al procesar el depósito (Simulado)"))
        } else {
            val commission = amount * 0.01
            val netAmount = amount - commission
            val formatter = NumberFormat.getCurrencyInstance(Locale("es", "CL"))
            Result.success("Depósito de ${formatter.format(amount)} exitoso. Neto (después de comisión ${formatter.format(commission)}): ${formatter.format(netAmount)} (Simulado)")
        }
    }
}

// --- ViewModel ---
@HiltViewModel
class DepositViewModel @Inject constructor(
    private val depositService: DepositService,
    private val pendingOperationRepository: PendingOperationRepository // Inyecta el repositorio
) : ViewModel() {

    private val _uiState = MutableStateFlow(DepositUiState())
    val uiState: StateFlow<DepositUiState> = _uiState.asStateFlow()

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
                amountError = null,
                operationResult = null,
                isError = false
            )
        }
    }

    fun onOnlineStatusChange(isOnline: Boolean) {
        _uiState.update { it.copy(isOnline = isOnline, operationResult = null, isError = false) }
    }

    fun performDeposit() {
        val currentAmountString = _uiState.value.amount
        if (currentAmountString.isBlank()) {
            _uiState.update { it.copy(amountError = "El monto no puede estar vacío.", operationResult = null, isError = true) }
            return
        }

        val amountValue = currentAmountString.toDoubleOrNull()
        if (amountValue == null || amountValue <= 0) {
            _uiState.update { it.copy(amountError = "Ingrese un monto válido.", operationResult = null, isError = true) }
            return
        }

        _uiState.update { it.copy(isLoading = true, operationResult = null, amountError = null, isError = false) }

        viewModelScope.launch {
            if (_uiState.value.isOnline) {
                // Modo Online: intentar procesar con el servicio
                val result = depositService.makeDeposit(amountValue, true) // Forzar isOnline = true para el servicio
                _uiState.update { currentState ->
                    result.fold(
                        onSuccess = { successMessage ->
                            currentState.copy(isLoading = false, operationResult = successMessage, isError = false, amount = "")
                        },
                        onFailure = { exception ->
                            currentState.copy(isLoading = false, operationResult = exception.message ?: "Error desconocido", isError = true)
                        }
                    )
                }
            } else {
                // Modo Offline: guardar en el repositorio de operaciones pendientes
                try {
                    pendingOperationRepository.addOperation(
                        type = "DEPOSIT",
                        data = mapOf(
                            "amount" to amountValue,
                            "currency" to "CLP", // Puedes añadir más datos relevantes
                            "timestamp_original" to System.currentTimeMillis()
                        )
                    )
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            operationResult = "Depósito guardado offline. Se sincronizará más tarde.",
                            isError = false, // No es un error, es un guardado pendiente
                            amount = "" // Limpiar el monto
                        )
                    }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            operationResult = "Error al guardar el depósito offline: ${e.message}",
                            isError = true
                        )
                    }
                }
            }
        }
    }
}
