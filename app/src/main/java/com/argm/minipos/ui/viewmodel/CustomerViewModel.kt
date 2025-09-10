package com.argm.minipos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.argm.minipos.data.model.Customer
import com.argm.minipos.data.repository.CustomerRepository
import com.argm.minipos.utils.UiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CustomerViewModel @Inject constructor(
    private val customerRepository: CustomerRepository
) : ViewModel() {

    private val _customers = MutableStateFlow<UiResult<List<Customer>>>(UiResult.Loading())
    val customers: StateFlow<UiResult<List<Customer>>> = _customers.asStateFlow()

    private val _selectedCustomer = MutableStateFlow<UiResult<Customer?>>(UiResult.Success(null))
    val selectedCustomer: StateFlow<UiResult<Customer?>> = _selectedCustomer.asStateFlow()

    private val _addCustomerResult = MutableStateFlow<UiResult<Customer>>(UiResult.Loading())
    val addCustomerResult: StateFlow<UiResult<Customer>> = _addCustomerResult.asStateFlow()
    
    init {
        loadAllCustomers()
    }

    fun loadAllCustomers() {
        viewModelScope.launch {
            _customers.value = UiResult.Loading()
            customerRepository.getAllCustomers()
                .catch { e -> _customers.value = UiResult.Error(e.message ?: "Error loading customers") }
                .collect { customerList ->
                    _customers.value = UiResult.Success(customerList)
                }
        }
    }

    fun getCustomerByRut(rut: String) {
        viewModelScope.launch {
            _selectedCustomer.value = UiResult.Loading()
            customerRepository.getCustomerByRut(rut)
                .catch { e -> _selectedCustomer.value = UiResult.Error(e.message ?: "Error fetching customer") }
                .collect{ customer ->
                    _selectedCustomer.value = UiResult.Success(customer)
                }
        }
    }

    fun addCustomer(rut: String, name: String?) {
        viewModelScope.launch {
            _addCustomerResult.value = UiResult.Loading()
            if (rut.isBlank()) {
                _addCustomerResult.value = UiResult.Error("RUT cannot be empty")
                return@launch
            }
            
            val customer = Customer(rut = rut, name = name, balance = 0.0)
            val result = customerRepository.addCustomer(customer) 
            _addCustomerResult.value = result

            if (result is UiResult.Success) {
                loadAllCustomers()
            }
        }
    }
    
    fun clearAddCustomerResult() {
        _addCustomerResult.value = UiResult.Loading() 
    }
}
