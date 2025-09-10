package com.argm.minipos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.argm.minipos.data.local.dao.SaleDao
import com.argm.minipos.data.model.SaleWithItems
import com.argm.minipos.data.repository.SaleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SalesHistoryUiState(
    val salesHistory: List<SaleWithItems> = emptyList(),
    val isLoadingHistory: Boolean = false,
    val salesHistoryErrorMessage: String? = null
)

@HiltViewModel
class SalesHistoryViewModel @Inject constructor(
    private val saleDao: SaleDao,
    private val saleRepository: SaleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SalesHistoryUiState(isLoadingHistory = true))
    val uiState: StateFlow<SalesHistoryUiState> = _uiState.asStateFlow()

    init {
        loadSalesHistory()
    }


    fun loadSalesHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingHistory = true, salesHistoryErrorMessage = null) }
            saleRepository.getSalesHistory()
                .catch { exception ->
                    _uiState.update {
                        it.copy(
                            isLoadingHistory = false,
                            salesHistoryErrorMessage = "Error al cargar historial: ${exception.message}"
                        )
                    }
                }
                .collect { historyList ->
                    _uiState.update {
                        it.copy(
                            isLoadingHistory = false,
                            salesHistory = historyList
                        )
                    }
                }
        }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(salesHistoryErrorMessage = null) }
    }
}