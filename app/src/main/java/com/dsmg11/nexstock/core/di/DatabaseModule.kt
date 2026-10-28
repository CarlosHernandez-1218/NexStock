package com.dsmg11.nexstock.core.di

import android.content.Context
import androidx.room.Room
import com.dsmg11.nexstock.data.local.NexStockDatabase
import com.dsmg11.nexstock.data.local.ProductDao
import com.dsmg11.nexstock.data.local.ReceptionDao
import com.dsmg11.nexstock.data.local.UserProfileDao
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
    fun provideDatabase(@ApplicationContext context: Context): NexStockDatabase =
        Room.databaseBuilder(context, NexStockDatabase::class.java, "nexstock.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideUserProfileDao(database: NexStockDatabase): UserProfileDao =
        database.userProfileDao()

    @Provides
    fun provideProductDao(database: NexStockDatabase): ProductDao =
        database.productDao()

    @Provides
    fun provideReceptionDao(database: NexStockDatabase): ReceptionDao =
        database.receptionDao()
}
