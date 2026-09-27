package com.dsmg11.nexstock.domain.usecase.auth

import com.dsmg11.nexstock.domain.model.User
import com.dsmg11.nexstock.domain.repository.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): Result<User> {
        if (email.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("Correo y contraseña son obligatorios"))
        }
        return authRepository.signIn(email.trim(), password)
    }
}
