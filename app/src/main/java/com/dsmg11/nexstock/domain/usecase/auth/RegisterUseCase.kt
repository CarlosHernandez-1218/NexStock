package com.dsmg11.nexstock.domain.usecase.auth

import com.dsmg11.nexstock.domain.model.User
import com.dsmg11.nexstock.domain.repository.AuthRepository
import javax.inject.Inject

class RegisterUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val ensureUserProfile: EnsureUserProfileUseCase
) {
    suspend operator fun invoke(email: String, password: String): Result<User> =
        authRepository.signUp(email.trim(), password)
            .onSuccess { user -> ensureUserProfile(user) }
}