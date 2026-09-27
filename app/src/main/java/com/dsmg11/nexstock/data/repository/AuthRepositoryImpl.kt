package com.dsmg11.nexstock.data.repository

import com.dsmg11.nexstock.domain.model.User
import com.dsmg11.nexstock.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth
) : AuthRepository {

    override val currentUser: User?
        get() = auth.currentUser?.toUser()

    override suspend fun signIn(email: String, password: String): Result<User> = runCatching {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        checkNotNull(result.user).toUser()
    }

    override suspend fun signUp(email: String, password: String): Result<User> = runCatching {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        checkNotNull(result.user).toUser()
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> = runCatching {
        auth.sendPasswordResetEmail(email).await()
        Unit
    }

    override fun signOut() = auth.signOut()
}

private fun FirebaseUser.toUser() = User(uid = uid, email = email)