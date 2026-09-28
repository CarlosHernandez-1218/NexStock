package com.dsmg11.nexstock.data.repository

import com.dsmg11.nexstock.domain.model.User
import com.dsmg11.nexstock.domain.repository.AuthRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth
) : AuthRepository {

    override val currentUser: User?
        get() = auth.currentUser?.toUser()

    override suspend fun signIn(email: String, password: String): Result<User> = safeCall {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        checkNotNull(result.user).toUser()
    }

    override suspend fun signUp(email: String, password: String): Result<User> = safeCall {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        checkNotNull(result.user).toUser()
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> = safeCall {
        auth.sendPasswordResetEmail(email).await()
        Unit
    }

    override fun signOut() = auth.signOut()

    // Ejecuta la llamada a Firebase y convierte cualquier error en un mensaje claro en español
    private suspend fun <T> safeCall(block: suspend () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(Exception(e.toSpanishMessage(), e))
        }
}

private fun FirebaseUser.toUser() = User(uid = uid, email = email)

private fun Exception.toSpanishMessage(): String = when (this) {
    // El orden importa: WeakPassword es un tipo de InvalidCredentials
    is FirebaseAuthWeakPasswordException -> "La contraseña es muy débil (mínimo 6 caracteres)"
    is FirebaseAuthUserCollisionException -> "Ya existe una cuenta con ese correo"
    is FirebaseAuthInvalidUserException -> "No existe una cuenta con ese correo"
    is FirebaseAuthInvalidCredentialsException -> "Correo o contraseña incorrectos"
    is FirebaseNetworkException -> "Sin conexión a internet"
    is FirebaseTooManyRequestsException -> "Demasiados intentos. Espera un momento e inténtalo de nuevo"
    else -> "Ocurrió un error inesperado. Inténtalo de nuevo"
}