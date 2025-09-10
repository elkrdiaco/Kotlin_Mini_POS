package com.argm.minipos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.argm.minipos.data.model.Customer // Asumiendo que tienes un modelo Customer
import com.argm.minipos.data.model.Product
import com.argm.minipos.data.model.Sale
import com.argm.minipos.data.model.SaleItem
import com.argm.minipos.data.repository.CustomerRepository // <<<--- AÑADIR IMPORTACIÓN
import com.argm.minipos.data.repository.ProductRepository
import com.argm.minipos.data.repository.SaleRepository
import com.argm.minipos.util.UiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull // Para obtener el valor actual del Flow
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
    val selectedCustomerRut: String? = null,
    val selectedCustomerName: String? = null, // Podrías añadir para mostrar nombre
    val showSaleSuccessDialog: Boolean = false
)

@HiltViewModel
class SalesViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val saleRepository: SaleRepository,
    private val customerRepository: CustomerRepository // <<<--- INYECTAR CustomerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SalesUiState())
    val uiState: StateFlow<SalesUiState> = _uiState.asStateFlow()

    init {
        loadProducts()
    }

    // loadProducts, addProductToCart, removeProductFromCart, updateCartItemQuantity, updateCartState, clearCart (sin cambios)
    // ... (estas funciones permanecen igual que en tu código original)
    fun loadProducts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, infoMessage = null, showSaleSuccessDialog = false)
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
        var infoMsg: String? = null

        if (existingCartItemIndex != -1) {
            val item = currentCart[existingCartItemIndex]
            val newQuantity = item.quantity + 1
            if (newQuantity <= productToAdd.stockQuantity) {
                currentCart[existingCartItemIndex] = item.copy(
                    quantity = newQuantity,
                    subtotal = BigDecimal.valueOf(productToAdd.price).multiply(BigDecimal(newQuantity))
                )
            } else {
                infoMsg = "Stock insuficiente para ${productToAdd.name}. En carrito: ${item.quantity}, Stock: ${productToAdd.stockQuantity}"
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
            } else {
                infoMsg = "Stock agotado para ${productToAdd.name}"
            }
        }
        _uiState.value = _uiState.value.copy(infoMessage = infoMsg, showSaleSuccessDialog = false)
        if (infoMsg == null) {
            updateCartState(currentCart)
        }
    }

    fun removeProductFromCart(itemToRemove: CartItem) {
        val currentCart = _uiState.value.cart.toMutableList()
        currentCart.removeAll { it.product.id == itemToRemove.product.id }
        _uiState.value = _uiState.value.copy(showSaleSuccessDialog = false)
        updateCartState(currentCart)
    }

    fun updateCartItemQuantity(itemProduct: Product, newQuantity: Int) {
        val currentCart = _uiState.value.cart.toMutableList()
        val existingCartItemIndex = currentCart.indexOfFirst { it.product.id == itemProduct.id }
        var infoMsg: String? = null

        if (existingCartItemIndex != -1) {
            val item = currentCart[existingCartItemIndex]
            if (newQuantity <= 0) {
                currentCart.removeAt(existingCartItemIndex)
            } else if (newQuantity <= item.product.stockQuantity) {
                currentCart[existingCartItemIndex] = item.copy(
                    quantity = newQuantity,
                    subtotal = BigDecimal.valueOf(item.product.price).multiply(BigDecimal(newQuantity))
                )
            } else {
                infoMsg = "Stock insuficiente para ${item.product.name}. Máximo: ${item.product.stockQuantity}, Solicitado: $newQuantity"
            }
        }
        _uiState.value = _uiState.value.copy(infoMessage = infoMsg, showSaleSuccessDialog = false)
        if (infoMsg == null || newQuantity <= 0) {
            updateCartState(currentCart)
        }
    }


    private fun updateCartState(newCart: List<CartItem>) {
        val newTotal = newCart.fold(BigDecimal.ZERO) { acc, cartItem ->
            acc.add(cartItem.subtotal)
        }
        if (_uiState.value.infoMessage == null || !_uiState.value.showSaleSuccessDialog) {
            _uiState.value = _uiState.value.copy(
                cart = newCart,
                cartTotal = newTotal.setScale(2, RoundingMode.HALF_UP),
                infoMessage = if (_uiState.value.showSaleSuccessDialog) _uiState.value.infoMessage else null
            )
        } else {
            _uiState.value = _uiState.value.copy(
                cart = newCart,
                cartTotal = newTotal.setScale(2, RoundingMode.HALF_UP)
            )
        }
    }


    fun clearCart() {
        _uiState.value = _uiState.value.copy(
            cart = emptyList(),
            cartTotal = BigDecimal.ZERO,
            infoMessage = null,
            // selectedCustomerRut = null, // Se limpia explícitamente tras venta exitosa o con función propia
            showSaleSuccessDialog = false
        )
    }


    fun selectCustomerForSale(rut: String?) {
        _uiState.value = _uiState.value.copy(selectedCustomerRut = rut, errorMessage = null, infoMessage = null, selectedCustomerName = null)
        if (rut != null) {
            viewModelScope.launch {
                // Opcional: Cargar nombre del cliente para mostrarlo en la UI de ventas
                val customer = customerRepository.getCustomerByRut(rut).firstOrNull() // Asumiendo que getCustomerByRut devuelve Flow
                _uiState.value = _uiState.value.copy(selectedCustomerName = customer?.name)
            }
        }
    }

    fun finalizeCurrentSale() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, infoMessage = null)
            val currentCartItems = _uiState.value.cart
            val currentCartTotal = _uiState.value.cartTotal
            val selectedRut = _uiState.value.selectedCustomerRut

            if (currentCartItems.isEmpty()) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "El carrito está vacío.", showSaleSuccessDialog = false)
                return@launch
            }

            // --- LÓGICA DE VALIDACIÓN Y DESCUENTO DE SALDO ---
            if (selectedRut != null) {
                val customer = customerRepository.getCustomerByRut(selectedRut).firstOrNull() // Asume Flow, ajusta si es suspend fun
                if (customer == null) {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Cliente no encontrado.", showSaleSuccessDialog = false)
                    return@launch
                }

                // Convertir BigDecimal a Double para comparación y deducción. Asegúrate que \`customer.balance\` sea Double.
                val saleTotalDouble = currentCartTotal.toDouble()
                if (customer.balance < saleTotalDouble) {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Saldo insuficiente. Saldo actual: ${customer.balance}, Total venta: $saleTotalDouble", showSaleSuccessDialog = false)
                    return@launch
                }

                // Intentar deducir el saldo
                val deductResult = customerRepository.deductBalanceFromCustomer(selectedRut, saleTotalDouble)
                when (deductResult) {
                    is UiResult.Error -> {
                        _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Error al descontar saldo: ${deductResult.message}", showSaleSuccessDialog = false)
                        return@launch
                    }
                    is UiResult.Success -> {
                        // Saldo descontado con éxito, continuar con la creación de la venta
                        _uiState.value = _uiState.value.copy(infoMessage = "Saldo descontado correctamente.") // Mensaje intermedio opcional
                    }
                    else -> { /* Podrías manejar Loading si deductBalanceFromCustomer lo emite */ }
                }
            }
            // --- FIN LÓGICA DE VALIDACIÓN Y DESCUENTO ---

            // Si llegamos aquí, o no había cliente seleccionado, o el saldo fue suficiente y descontado.
            val saleToSave = Sale(
                timestamp = Date(),
                totalAmount = currentCartTotal,
                customerRut = selectedRut
            )
            val saleItemsToSave = currentCartItems.map {
                SaleItem(
                    saleId = 0,
                    productId = it.product.id,
                    productName = it.product.name,
                    quantity = it.quantity,
                    priceAtSale = BigDecimal.valueOf(it.product.price),
                    subtotal = it.subtotal
                )
            }

            // Llamar a saleRepository para guardar la venta
            // selectedRut se pasa igual, puede ser útil para el repositorio aunque ya hayamos manejado el saldo.
            when (val result = saleRepository.finalizeSale(saleToSave, saleItemsToSave, selectedRut)) {
                is UiResult.Success -> {
                    clearCart()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        infoMessage = "¡Venta finalizada con éxito! ID: ${result.data}",
                        errorMessage = null,
                        showSaleSuccessDialog = true,
                        selectedCustomerRut = null, // Limpiar cliente seleccionado
                        selectedCustomerName = null
                    )
                }
                is UiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message ?: "Error desconocido al finalizar la venta.",
                        infoMessage = null, // Limpiar mensaje de descuento si lo hubo
                        showSaleSuccessDialog = false
                        // Considerar si se debe revertir el descuento de saldo si la venta falla AQUÍ.
                        // Esto requeriría una lógica de compensación (ej. customerRepository.addBalanceToCustomer(...))
                        // Por simplicidad, esta propuesta no lo incluye.
                    )
                }
                is UiResult.Loading -> {
                     _uiState.value = _uiState.value.copy(isLoading = true) // Mantener isLoading
                }
            }
        }
    }

    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun clearInfoMessage() {
        if (!_uiState.value.showSaleSuccessDialog) { // Solo limpiar si el diálogo de éxito no está activo
            _uiState.value = _uiState.value.copy(infoMessage = null)
        }
    }

    fun dismissSaleSuccessDialog() {
        _uiState.value = _uiState.value.copy(
            showSaleSuccessDialog = false,
            infoMessage = null
        )
    }
}
