package com.dsmg11.nexstock.domain.repository

import com.dsmg11.nexstock.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface UserProfileRepository {
    fun observeProfile(uid: String): Flow<UserProfile?>
    suspend fun createProfileIfMissing(uid: String, email: String): Result<Unit>
    suspend fun updateBasicInfo(uid: String, fullName: String, phone: String): Result<Unit>
}