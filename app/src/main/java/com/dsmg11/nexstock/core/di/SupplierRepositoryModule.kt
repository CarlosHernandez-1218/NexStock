package com.dsmg11.nexstock.core.di

import com.dsmg11.nexstock.data.repository.SupplierRepositoryImpl
import com.dsmg11.nexstock.domain.repository.SupplierRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SupplierRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSupplierRepository(
        supplierRepositoryImpl: SupplierRepositoryImpl
    ): SupplierRepository
}