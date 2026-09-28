package com.dsmg11.nexstock.domain.repository

import com.dsmg11.nexstock.domain.model.ProductLine
import com.dsmg11.nexstock.domain.model.UserProfile
import com.dsmg11.nexstock.domain.model.UserRole
import kotlinx.coroutines.flow.Flow

interface UserProfileRepository {
    fun observeProfile(uid: String): Flow<UserProfile?>
    suspend fun createProfileIfMissing(uid: String, email: String): Result<Unit>
    suspend fun updateBasicInfo(uid: String, fullName: String, phone: String): Result<Unit>
    fun observeAllProfiles(): Flow<List<UserProfile>>
    suspend fun updateRoleAndLines(uid: String, role: UserRole, lines: List<ProductLine>): Result<Unit>
    suspend fun uploadProfilePhoto(uid: String, localUri: String): Result<String>
}