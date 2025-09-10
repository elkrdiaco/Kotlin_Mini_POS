package com.argm.minipos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.argm.minipos.data.model.Customer
import com.argm.minipos.data.repository.CustomerRepository
import com.argm.minipos.util.UiResult
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

    private val _addCustomerResult = MutableStateFlow<UiResult<Customer>>(UiResult.Loading())
    val addCustomerResult: StateFlow<UiResult<Customer>> = _addCustomerResult.asStateFlow()
    
    private val _recordDepositResult = MutableStateFlow<UiResult<Unit>>(UiResult.Loading())

    private val _syncDepositsResult = MutableStateFlow<UiResult<String>>(UiResult.Loading()) // String para el mensaje de éxito/error

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
            // customerRepository.addCustomer now returns UiResult<Customer> directly
            val result = customerRepository.addCustomer(customer) 
            _addCustomerResult.value = result // Assign the result directly

            if (result is UiResult.Success) {
                loadAllCustomers() // Recargar la lista de clientes
            }
        }
    }
    
    fun clearAddCustomerResult() {
        // Reset to Loading or a specific "Idle" or "Cleared" state if you add one to UiResult
        _addCustomerResult.value = UiResult.Loading() 
    }

    fun recordDeposit(customerRut: String, amount: Double, isOffline: Boolean) {
        viewModelScope.launch {
            _recordDepositResult.value = UiResult.Loading()
            if (customerRut.isBlank() || amount <= 0) {
                _recordDepositResult.value = UiResult.Error("Invalid customer RUT or amount.")
                return@launch
            }

            val result = customerRepository.recordDeposit(customerRut, amount, isOffline)
            _recordDepositResult.value = result

            if (result is UiResult.Success && !isOffline) {
                if (_selectedCustomer.value is UiResult.Success && (_selectedCustomer.value as UiResult.Success<Customer?>).data?.rut == customerRut) {
                    getCustomerByRut(customerRut)
                }
                loadAllCustomers()
            }
        }
    }

    fun clearRecordDepositResult() {
         _recordDepositResult.value = UiResult.Loading()
    }

    fun syncPendingDeposits(customerRut: String) {
        viewModelScope.launch {
            _syncDepositsResult.value = UiResult.Loading()
            if (customerRut.isBlank()) {
                _syncDepositsResult.value = UiResult.Error("Customer RUT cannot be empty.")
                return@launch
            }
            val result = customerRepository.syncPendingDepositsForCustomer(customerRut)
            _syncDepositsResult.value = result

            if (result is UiResult.Success) {
                if (_selectedCustomer.value is UiResult.Success && (_selectedCustomer.value as UiResult.Success<Customer?>).data?.rut == customerRut) {
                   getCustomerByRut(customerRut)
                }
                // Also reload all customers as sync changes balances visible in the list
                loadAllCustomers()
            }
        }
    }

    fun clearSyncDepositsResult() {
        _syncDepositsResult.value = UiResult.Loading()
    }

}
