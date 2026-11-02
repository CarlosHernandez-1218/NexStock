package com.dsmg11.nexstock.core.di

import com.dsmg11.nexstock.data.notification.ExtensionAlertRepositoryImpl
import com.dsmg11.nexstock.domain.repository.ExtensionAlertRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ExtensionAlertRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindExtensionAlertRepository(
        impl: ExtensionAlertRepositoryImpl
    ): ExtensionAlertRepository
}