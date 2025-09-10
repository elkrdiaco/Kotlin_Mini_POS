package com.argm.minipos.ui.balance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

// TODO: Move to a dedicated service file if this grows
interface BalanceService {
    suspend fun getBalance(accountId: String): Result<String>
}

class SimulatedBalanceService @Inject constructor() : BalanceService {
    override suspend fun getBalance(accountId: String): Result<String> {
        delay(Random.nextLong(500, 801)) // Simulate network delay
        return if (Random.nextFloat() < 0.2) { // 20% chance of error
            Result.failure(Exception("Error al obtener el saldo (Simulado)"))
        } else {
            // Simulate different balances based on accountId or just a fixed one
            if (accountId.isBlank()) {
                 Result.failure(IllegalArgumentException("El ID de cuenta no puede estar vacío"))
            } else {
                Result.success("Saldo para $accountId: \$${Random.nextInt(1000, 50000)} CLP (Simulado)")
            }
        }
    }
}

data class BalanceUiState(
    val accountId: String = "",
    val balance: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val accountIdError: String? = null
)

@HiltViewModel
class BalanceViewModel @Inject constructor(
    private val balanceService: BalanceService
) : ViewModel() {

    private val _uiState = MutableStateFlow(BalanceUiState())
    val uiState: StateFlow<BalanceUiState> = _uiState.asStateFlow()

    fun onAccountIdChange(newAccountId: String) {
        _uiState.update { currentState ->
            currentState.copy(
                accountId = newAccountId,
                accountIdError = null // Clear previous error on new input
            )
        }
    }

    fun fetchBalance() {
        val currentAccountId = _uiState.value.accountId
        if (currentAccountId.isBlank()) {
            _uiState.update { it.copy(accountIdError = "El RUT o N° de cuenta no puede estar vacío.") }
            return
        }
        // Basic validation for RUT-like structure (very simplified)
        // A more robust validation would be needed for a real app
        if (!currentAccountId.matches(Regex("""^(\d{1,8}-?[\dkK])$"""))) { // Changed to raw string
             _uiState.update { it.copy(accountIdError = "Formato de RUT o N° de cuenta inválido.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, balance = null, accountIdError = null) }
            val result = balanceService.getBalance(currentAccountId)
            _uiState.update { currentState ->
                result.fold(
                    onSuccess = { balanceValue ->
                        currentState.copy(isLoading = false, balance = balanceValue)
                    },
                    onFailure = { exception ->
                        currentState.copy(isLoading = false, error = exception.message ?: "Error desconocido")
                    }
                )
            }
        }
    }
}
