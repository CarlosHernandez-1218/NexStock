package com.dsmg11.nexstock.core.di

import com.dsmg11.nexstock.data.repository.PushRepositoryImpl
import com.dsmg11.nexstock.domain.repository.PushRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PushRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPushRepository(impl: PushRepositoryImpl): PushRepository
}
