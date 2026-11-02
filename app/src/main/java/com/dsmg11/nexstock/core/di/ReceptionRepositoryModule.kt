package com.dsmg11.nexstock.core.di

import com.dsmg11.nexstock.data.repository.ReceptionRepositoryImpl
import com.dsmg11.nexstock.domain.repository.ReceptionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ReceptionRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindReceptionRepository(impl: ReceptionRepositoryImpl): ReceptionRepository
}
