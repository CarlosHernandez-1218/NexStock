package com.dsmg11.nexstock.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {

    @Query("SELECT * FROM user_profiles WHERE uid = :uid")
    fun observe(uid: String): Flow<UserProfileEntity?>
    @Query("UPDATE user_profiles SET fullName = :fullName, phone = :phone WHERE uid = :uid")
    suspend fun updateBasicInfo(uid: String, fullName: String, phone: String)
    @Upsert
    suspend fun upsert(profile: UserProfileEntity)
}