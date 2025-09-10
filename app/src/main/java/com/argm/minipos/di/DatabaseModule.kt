package com.argm.minipos.di

import android.content.Context
import androidx.room.Room
import com.argm.minipos.data.local.AppDatabase
import com.argm.minipos.data.local.dao.CustomerDao
import com.argm.minipos.data.local.dao.PendingDepositDao
import com.argm.minipos.data.local.dao.ProductDao
import com.argm.minipos.data.local.dao.SaleDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext appContext: Context): AppDatabase {
        return Room.databaseBuilder(
            appContext,
            AppDatabase::class.java,
            "minipos_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideSaleDao(appDatabase: AppDatabase): SaleDao {
        return appDatabase.saleDao()
    }

    @Provides
    fun provideProductDao(appDatabase: AppDatabase): ProductDao {
        return appDatabase.productDao()
    }

    @Provides
    fun provideCustomerDao(appDatabase: AppDatabase): CustomerDao {
        return appDatabase.customerDao()
    }

    @Provides
    fun providePendingDepositDao(appDatabase: AppDatabase): PendingDepositDao {
        return appDatabase.pendingDepositDao()
    }
}
