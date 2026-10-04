package com.dsmg11.nexstock.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [UserProfileEntity::class, ProductEntity::class],
    version = 3,
    exportSchema = false
)
abstract class NexStockDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun productDao(): ProductDao
}