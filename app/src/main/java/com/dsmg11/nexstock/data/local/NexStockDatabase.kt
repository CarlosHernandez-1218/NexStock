package com.dsmg11.nexstock.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [UserProfileEntity::class],
    version = 1,
    exportSchema = false
)
abstract class NexStockDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
}