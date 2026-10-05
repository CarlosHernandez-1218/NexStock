package com.dsmg11.nexstock.domain.usecase.auth

import com.dsmg11.nexstock.domain.model.User
import com.dsmg11.nexstock.domain.repository.AuthRepository
import javax.inject.Inject

class GoogleSignInUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val ensureUserProfile: EnsureUserProfileUseCase
) {

    suspend operator fun invoke(idToken: String): Result<User> {
        if (idToken.isBlank()) {
            return Result.failure(
                IllegalArgumentException("No se obtuvo la credencial de Google")
            )
        }

        return authRepository.signInWithGoogle(idToken)
            .onSuccess { user ->
                ensureUserProfile(user)
            }
    }
}