package com.dsmg11.nexstock.core.di

import com.dsmg11.nexstock.data.repository.PurchaseOrderRepositoryImpl
import com.dsmg11.nexstock.domain.repository.PurchaseOrderRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PurchaseOrderRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPurchaseOrderRepository(
        impl: PurchaseOrderRepositoryImpl
    ): PurchaseOrderRepository
}