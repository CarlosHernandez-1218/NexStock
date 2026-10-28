package com.dsmg11.nexstock.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [UserProfileEntity::class, ProductEntity::class, ReceptionEntity::class],
    version = 4,
    exportSchema = false
)
abstract class NexStockDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun productDao(): ProductDao
    abstract fun receptionDao(): ReceptionDao
}
