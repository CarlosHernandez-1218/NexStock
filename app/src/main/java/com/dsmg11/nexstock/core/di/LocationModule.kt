package com.dsmg11.nexstock.core.di

import com.dsmg11.nexstock.data.location.DeviceLocationRepositoryImpl
import com.dsmg11.nexstock.domain.repository.DeviceLocationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LocationModule {

    @Binds
    @Singleton
    abstract fun bindDeviceLocationRepository(
        impl: DeviceLocationRepositoryImpl
    ): DeviceLocationRepository
}