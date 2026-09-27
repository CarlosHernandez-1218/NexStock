package com.dsmg11.nexstock.domain.usecase.auth

import com.dsmg11.nexstock.domain.repository.AuthRepository
import javax.inject.Inject

class ForgotPasswordUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String): Result<Unit> {
        if (email.isBlank()) {
            return Result.failure(IllegalArgumentException("Ingresa un correo válido"))
        }
        return authRepository.sendPasswordReset(email.trim())
    }
}
