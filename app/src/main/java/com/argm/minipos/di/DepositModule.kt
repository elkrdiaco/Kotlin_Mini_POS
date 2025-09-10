package com.argm.minipos.di

import com.argm.minipos.ui.balance.BalanceService
import com.argm.minipos.ui.balance.SimulatedBalanceService
import com.argm.minipos.ui.deposit.DepositService
import com.argm.minipos.ui.deposit.SimulatedDepositService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class) // Or an appropriate component
abstract class DepositModule {

    @Binds
    @Singleton // Hace que BalanceService sea un singleton
    abstract fun bindBalanceService(
        simulatedBalanceService: SimulatedBalanceService
    ): BalanceService

    @Binds
    @Singleton // Hace que DepositService sea un singleton
    abstract fun bindDepositService(
        simulatedDepositService: SimulatedDepositService
    ): DepositService
}
