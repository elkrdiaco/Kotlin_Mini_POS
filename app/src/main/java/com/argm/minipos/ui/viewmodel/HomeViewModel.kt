package com.argm.minipos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.argm.minipos.data.model.Product
import com.argm.minipos.data.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class HomeUiState(
    val products: List<Product> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadProducts()
    }

    fun loadProducts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            productRepository.getAllProducts()
                .catch { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Error al cargar productos: ${exception.localizedMessage}"
                        )
                    }
                }
                .collect { productList ->
                    _uiState.update {
                        it.copy(
                            products = productList,
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun addSampleProduct() {
        viewModelScope.launch {
            try {
                val sampleProduct = Product(
                    id = UUID.randomUUID().toString(),
                    name = "Producto de Muestra ${UUID.randomUUID().toString().take(4)}",
                    price = (10..100).random().toDouble(),
                    stockQuantity = (1..20).random(),
                    category = if (Math.random() > 0.5) "Electrónicos" else "Ropa"
                )
                productRepository.insertProduct(sampleProduct)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Error al añadir producto de muestra: ${e.localizedMessage}")
                }
            }
        }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}