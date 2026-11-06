package com.dsmg11.nexstock.core.di

import com.dsmg11.nexstock.data.repository.SupplierReturnRepositoryImpl
import com.dsmg11.nexstock.domain.repository.SupplierReturnRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SupplierReturnRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSupplierReturnRepository(
        impl: SupplierReturnRepositoryImpl
    ): SupplierReturnRepository
}