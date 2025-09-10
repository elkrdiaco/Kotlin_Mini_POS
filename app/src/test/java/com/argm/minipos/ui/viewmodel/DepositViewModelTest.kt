package com.argm.minipos.ui.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.argm.minipos.data.model.Customer
import com.argm.minipos.data.repository.CustomerRepository
import com.argm.minipos.data.repository.PendingOperationRepository
import com.argm.minipos.utils.UiResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations

@ExperimentalCoroutinesApi
class DepositViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var customerRepository: CustomerRepository

    @Mock
    private lateinit var depositService: DepositService

    @Mock
    private lateinit var pendingOperationRepository: PendingOperationRepository

    private lateinit var viewModel: DepositViewModel

    private val testRut = "12345678-9"
    private val testCustomerName = "Test Customer"

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        viewModel = DepositViewModel(customerRepository, depositService, pendingOperationRepository)
        `when`(customerRepository.getCustomerByRut(testRut)).thenReturn(flowOf(
            Customer(
                rut = testRut,
                name = testCustomerName,
                balance = 0.0
            )))
        viewModel.onCustomerSelected(testRut)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `performDeposit with valid amount (online) should succeed`() = runTest(testDispatcher) {
        val amount = 5000.0
        viewModel.onAmountChange(amount.toString())
        viewModel.onOnlineStatusChange(true)

        `when`(depositService.makeDeposit(amount, true)).thenReturn(Result.success("Server success"))
        `when`(customerRepository.addBalanceToCustomer(testRut, amount - 200.0)).thenReturn(UiResult.Success(Unit))

        viewModel.performDeposit()
        testDispatcher.scheduler.advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertFalse(uiState.isLoading)
        assertTrue(uiState.depositSuccess)
        assertFalse(uiState.isError)
        assertNotNull(uiState.message)
        assertTrue(uiState.message!!.contains("completado y sincronizado"))
        assertEquals("", uiState.amount)

        verify(depositService).makeDeposit(amount, true)
        verify(customerRepository).addBalanceToCustomer(testRut, amount - 200.0)
    }

    @Test
    fun `performDeposit with amount less than 1000 should show error`() = runTest(testDispatcher) {
        val amount = "500"
        viewModel.onAmountChange(amount)
        viewModel.performDeposit()

        val uiState = viewModel.uiState.value
        assertFalse(uiState.isLoading)
        assertTrue(uiState.isError)
        assertEquals("El monto mínimo del depósito es de 1000.", uiState.message)
        assertFalse(uiState.depositSuccess)
    }

    @Test
    fun `performDeposit with amount greater than 200000 should show error`() = runTest(testDispatcher) {
        val amount = "250000"
        viewModel.onAmountChange(amount)
        viewModel.performDeposit()

        val uiState = viewModel.uiState.value
        assertFalse(uiState.isLoading)
        assertTrue(uiState.isError)
        assertEquals("El monto máximo del depósito es de 200.000.", uiState.message)
        assertFalse(uiState.depositSuccess)
    }

    @Test
    fun `performDeposit with amount less than commission (online) should show error`() = runTest(testDispatcher) {
        val amount = "150"
        viewModel.onAmountChange(amount)
        viewModel.onOnlineStatusChange(true)

        viewModel.onAmountChange("1500")
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onAmountChange(amount)
         testDispatcher.scheduler.advanceUntilIdle()


        viewModel.performDeposit()
        testDispatcher.scheduler.advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertFalse(uiState.isLoading)
        assertTrue(uiState.isError)
        assertEquals("El monto mínimo del depósito es de 1000.", uiState.message)
        assertFalse(uiState.depositSuccess)
    }
    
    @Test
    fun `performDeposit with valid amount but less than commission (online) should show error - direct path`() = runTest(testDispatcher) {
        viewModel.onAmountChange("150")
        viewModel.onOnlineStatusChange(true)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.performDeposit()
        testDispatcher.scheduler.advanceUntilIdle()

        val finalUiState = viewModel.uiState.value
        assertFalse("isLoading should be false", finalUiState.isLoading)
        assertTrue("isError should be true", finalUiState.isError)
        assertEquals("Error message should be for minimum deposit", "El monto mínimo del depósito es de 1000.", finalUiState.message)
        assertFalse("depositSuccess should be false", finalUiState.depositSuccess)
    }


    @Test
    fun `onAmountChange with valid value should update amount and clear messages`() = runTest(testDispatcher) {
        viewModel.performDeposit()
        viewModel.clearCustomerSelection()
        viewModel.onAmountChange("")
        viewModel.performDeposit()

        assertTrue(viewModel.uiState.value.isError)
        assertNotNull(viewModel.uiState.value.message)

        val newAmount = "123.45"
        viewModel.onAmountChange(newAmount)
        testDispatcher.scheduler.advanceUntilIdle()


        val uiState = viewModel.uiState.value
        assertEquals(newAmount, uiState.amount)
        assertNull(uiState.message)
        assertFalse(uiState.isError)
        assertFalse(uiState.depositSuccess)
    }
    
    @Test
    fun `onAmountChange should format input correctly`() = runTest(testDispatcher) {
        viewModel.onAmountChange("1a2b3.4c5d6")
        assertEquals("123.45", viewModel.uiState.value.amount)

        viewModel.onAmountChange("12345")
        assertEquals("12345", viewModel.uiState.value.amount)

        viewModel.onAmountChange("123.45.67")
        assertEquals("123.45", viewModel.uiState.value.amount)
        
        viewModel.onAmountChange(".12")
        assertEquals(".12", viewModel.uiState.value.amount)

        viewModel.onAmountChange("123.")
        assertEquals("123.", viewModel.uiState.value.amount)
    }


    @Test
    fun `clearCustomerSelection should clear customer details and messages`() = runTest(testDispatcher) {
        viewModel.onCustomerSelected(testRut)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onAmountChange("1000")
        viewModel.performDeposit()
         `when`(depositService.makeDeposit(1000.0, true)).thenReturn(Result.success("Server success"))
        `when`(customerRepository.addBalanceToCustomer(testRut, 1000.0 - 200.0)).thenReturn(UiResult.Success(Unit))
        testDispatcher.scheduler.advanceUntilIdle()


        assertNotNull(viewModel.uiState.value.selectedCustomerRut)

        viewModel.clearCustomerSelection()
        testDispatcher.scheduler.advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertNull(uiState.selectedCustomerRut)
        assertNull(uiState.selectedCustomerName)
        assertNull(uiState.message)
        assertFalse(uiState.isError)
        assertFalse(uiState.depositSuccess)
    }
}
