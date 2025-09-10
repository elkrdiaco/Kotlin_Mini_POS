package com.argm.minipos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.argm.minipos.data.model.Product
import com.argm.minipos.data.model.Sale
import com.argm.minipos.data.model.SaleItem
import com.argm.minipos.data.repository.ProductRepository
import com.argm.minipos.data.repository.SaleRepository
import com.argm.minipos.util.UiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Date
import javax.inject.Inject

data class CartItem(
    val product: Product,
    val quantity: Int,
    val subtotal: BigDecimal
)

data class SalesUiState(
    val products: List<Product> = emptyList(),
    val cart: List<CartItem> = emptyList(),
    val cartTotal: BigDecimal = BigDecimal.ZERO,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
)

@HiltViewModel
class SalesViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val saleRepository: SaleRepository // Inyectar SaleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SalesUiState())
    val uiState: StateFlow<SalesUiState> = _uiState.asStateFlow()

    init {
        loadProducts()
    }

    fun loadProducts() {
        viewModelScope.launch {
            _uiState.value =
                _uiState.value.copy(isLoading = true, errorMessage = null, infoMessage = null)
            productRepository.getAllProducts()
                .catch { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Error al cargar productos: ${exception.message}"
                    )
                }
                .collect { productList ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        products = productList
                    )
                }
        }
    }

    fun addProductToCart(productToAdd: Product) {
        val currentCart = _uiState.value.cart.toMutableList()
        val existingCartItemIndex = currentCart.indexOfFirst { it.product.id == productToAdd.id }

        if (existingCartItemIndex != -1) {
            val item = currentCart[existingCartItemIndex]
            val newQuantity = item.quantity + 1
            if (newQuantity <= productToAdd.stockQuantity) {
                currentCart[existingCartItemIndex] = item.copy(
                    quantity = newQuantity,
                    subtotal = BigDecimal.valueOf(productToAdd.price)
                        .multiply(BigDecimal(newQuantity))
                )
                _uiState.value = _uiState.value.copy(infoMessage = null)
            } else {
                _uiState.value =
                    _uiState.value.copy(infoMessage = "Stock insuficiente para ${productToAdd.name}. En carrito: ${item.quantity}, Stock: ${productToAdd.stockQuantity}")
                return
            }
        } else {
            if (1 <= productToAdd.stockQuantity) {
                currentCart.add(
                    CartItem(
                        product = productToAdd,
                        quantity = 1,
                        subtotal = BigDecimal.valueOf(productToAdd.price)
                    )
                )
                _uiState.value = _uiState.value.copy(infoMessage = null)
            } else {
                _uiState.value =
                    _uiState.value.copy(infoMessage = "Stock agotado para ${productToAdd.name}")
                return
            }
        }
        updateCartState(currentCart)
    }

    fun removeProductFromCart(itemToRemove: CartItem) {
        val currentCart = _uiState.value.cart.toMutableList()
        currentCart.removeAll { it.product.id == itemToRemove.product.id }
        updateCartState(currentCart)
    }

    fun updateCartItemQuantity(itemProduct: Product, newQuantity: Int) {
        val currentCart = _uiState.value.cart.toMutableList()
        val existingCartItemIndex = currentCart.indexOfFirst { it.product.id == itemProduct.id }

        if (existingCartItemIndex != -1) {
            val item = currentCart[existingCartItemIndex]
            if (newQuantity <= 0) {
                currentCart.removeAt(existingCartItemIndex)
            } else if (newQuantity <= item.product.stockQuantity) {
                currentCart[existingCartItemIndex] = item.copy(
                    quantity = newQuantity,
                    subtotal = BigDecimal.valueOf(item.product.price)
                        .multiply(BigDecimal(newQuantity))
                )
                _uiState.value = _uiState.value.copy(infoMessage = null)
            } else {
                _uiState.value =
                    _uiState.value.copy(infoMessage = "Stock insuficiente para ${item.product.name}. Máximo: ${item.product.stockQuantity}, Solicitado: $newQuantity")
                return
            }
            updateCartState(currentCart)
        }
    }

    private fun updateCartState(newCart: List<CartItem>) {
        val newTotal = newCart.fold(BigDecimal.ZERO) { acc, cartItem ->
            acc.add(cartItem.subtotal)
        }
        _uiState.value = _uiState.value.copy(
            cart = newCart,
            cartTotal = newTotal.setScale(2, RoundingMode.HALF_UP)
        )
    }

    fun clearCart() {
        _uiState.value = _uiState.value.copy(
            cart = emptyList(),
            cartTotal = BigDecimal.ZERO,
            infoMessage = null
        )
    }

    fun finalizeCurrentSale() {
        viewModelScope.launch {
            val currentCartItems = _uiState.value.cart
            val currentCartTotal = _uiState.value.cartTotal

            if (currentCartItems.isEmpty()) {
                _uiState.value = _uiState.value.copy(errorMessage = "El carrito está vacío.")
                return@launch
            }

            val saleToSave = Sale(
                id = 0, // El DAO se encargará
                timestamp = Date(),
                totalAmount = currentCartTotal
            )

            val saleItemsToSave = currentCartItems.map { cartItem ->
                SaleItem(
                    saleId = 0,
                    productId = cartItem.product.id,
                    productName = cartItem.product.name,
                    quantity = cartItem.quantity,
                    priceAtSale = BigDecimal.valueOf(cartItem.product.price),
                    subtotal = cartItem.subtotal
                )
            }

            when (val result = saleRepository.finalizeSale(saleToSave, saleItemsToSave)) {
                is UiResult.Success -> {
                    clearCart()
                    _uiState.value = _uiState.value.copy(
                        infoMessage = "¡Venta finalizada con éxito!",
                        errorMessage = null
                        // isLoadingSale = false
                    )
                }

                is UiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = result.message ?: "Error desconocido al finalizar la venta.",
                        infoMessage = null
                    )
                }
            }
        }
    }

    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun clearInfoMessage() {
        _uiState.value = _uiState.value.copy(infoMessage = null)
    }
}
