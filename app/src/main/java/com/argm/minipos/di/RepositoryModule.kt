package com.argm.minipos.di

import com.argm.minipos.data.repository.InMemoryPendingOperationRepository
import com.argm.minipos.data.repository.PendingOperationRepository
import com.argm.minipos.data.repository.ProductRepository
import com.argm.minipos.data.repository.ProductRepositoryImpl
import com.argm.minipos.data.repository.SaleRepository
import com.argm.minipos.data.repository.SaleRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindProductRepository(
        productRepositoryImpl: ProductRepositoryImpl
    ): ProductRepository

    @Binds
    @Singleton
    abstract fun bindSaleRepository(
        saleRepositoryImpl: SaleRepositoryImpl
    ): SaleRepository

    @Binds
    @Singleton
    abstract fun bindPendingOperationRepository(
        inMemoryPendingOperationRepository: InMemoryPendingOperationRepository
    ): PendingOperationRepository
}