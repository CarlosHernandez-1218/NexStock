package com.dsmg11.nexstock.domain.repository

import com.dsmg11.nexstock.domain.model.User

interface AuthRepository {
    val currentUser: User?
    suspend fun signIn(email: String, password: String): Result<User>
    suspend fun signUp(email: String, password: String): Result<User>
    suspend fun sendPasswordReset(email: String): Result<Unit>
    fun signOut()
}