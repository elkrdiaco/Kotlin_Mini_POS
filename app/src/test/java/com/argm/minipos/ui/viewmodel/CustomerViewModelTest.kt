package com.argm.minipos.ui.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.argm.minipos.data.repository.CustomerRepository
import com.argm.minipos.utils.UiResult
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class CustomerViewModelTest {

    @get:Rule
    var rule: InstantTaskExecutorRule = InstantTaskExecutorRule()

    @RelaxedMockK
    private lateinit var customerRepository: CustomerRepository

    private lateinit var customerViewModel: CustomerViewModel

    @Before
    fun onBefore() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(Dispatchers.Unconfined)
        customerViewModel = CustomerViewModel(customerRepository)
    }

    @After
    fun onAfter() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadAllCustomers updates customers state flow with success`() {
        // Given
        val mockCustomers = listOf(
            com.argm.minipos.data.model.Customer("123-4", "Customer 1", 100.0),
            com.argm.minipos.data.model.Customer("567-8", "Customer 2", 200.0)
        )
        coEvery { customerRepository.getAllCustomers() } returns flowOf(mockCustomers)

        // When
        customerViewModel.loadAllCustomers()

        // Then
        assertEquals(UiResult.Success(mockCustomers), customerViewModel.customers.value)
    }

    @Test
    fun `getCustomerByRut updates selectedCustomer state flow with success`() {
        // Given
        val rut = "123-4"
        val mockCustomer = com.argm.minipos.data.model.Customer(rut, "Customer 1", 100.0)
        coEvery { customerRepository.getCustomerByRut(rut) } returns flowOf(mockCustomer)

        // When
        customerViewModel.getCustomerByRut(rut)

        // Then
        assertEquals(UiResult.Success(mockCustomer), customerViewModel.selectedCustomer.value)
    }
    
    @Test
    fun `addCustomer with blank RUT returns error`() {
        // When
        customerViewModel.addCustomer("", "Test Customer")

        // Then
        assertTrue(customerViewModel.addCustomerResult.value is UiResult.Error)
        assertEquals("RUT cannot be empty", (customerViewModel.addCustomerResult.value as UiResult.Error).message)
    }

    @Test
    fun `addCustomer successfully adds customer and reloads all customers`() {
        // Given
        val rut = "123-4"
        val name = "Test Customer"
        val newCustomer = com.argm.minipos.data.model.Customer(rut = rut, name = name, balance = 0.0)
        coEvery { customerRepository.addCustomer(newCustomer) } returns UiResult.Success(newCustomer)
        coEvery { customerRepository.getAllCustomers() } returns flowOf(listOf(newCustomer))


        // When
        customerViewModel.addCustomer(rut, name)

        // Then
        assertEquals(UiResult.Success(newCustomer), customerViewModel.addCustomerResult.value)
        // Verify that loadAllCustomers was called, which updates the customers StateFlow
        assertEquals(UiResult.Success(listOf(newCustomer)), customerViewModel.customers.value)
    }
    
    @Test
    fun `clearAddCustomerResult resets addCustomerResult state`() {
        // Given
        // Set some initial state to verify it gets cleared
        customerViewModel.addCustomer("123-4", "Initial Customer")
        
        // When
        customerViewModel.clearAddCustomerResult()

        // Then
        // Assert that the state is reset to Loading or whatever the initial state of _addCustomerResult is
        assertTrue(customerViewModel.addCustomerResult.value is UiResult.Loading)
    }
}
