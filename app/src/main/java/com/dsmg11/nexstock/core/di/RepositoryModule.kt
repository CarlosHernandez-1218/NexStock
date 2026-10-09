package com.dsmg11.nexstock.core.di

import com.dsmg11.nexstock.data.repository.AuthRepositoryImpl
import com.dsmg11.nexstock.data.repository.MasterDataRepositoryImpl
import com.dsmg11.nexstock.data.repository.ProductRepositoryImpl
import com.dsmg11.nexstock.data.repository.UserProfileRepositoryImpl
import com.dsmg11.nexstock.domain.repository.AuthRepository
import com.dsmg11.nexstock.domain.repository.MasterDataRepository
import com.dsmg11.nexstock.domain.repository.ProductRepository
import com.dsmg11.nexstock.domain.repository.UserProfileRepository
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
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
    @Binds
    @Singleton
    abstract fun bindUserProfileRepository(impl: UserProfileRepositoryImpl): UserProfileRepository
    @Binds
    @Singleton
    abstract fun bindProductRepository(impl: ProductRepositoryImpl): ProductRepository
    @Binds
    @Singleton
    abstract fun bindMasterDataRepository(impl: MasterDataRepositoryImpl): MasterDataRepository
}