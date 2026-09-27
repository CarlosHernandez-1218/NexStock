package com.dsmg11.nexstock.domain.usecase

import com.dsmg11.nexstock.domain.repository.AuthRepository
import javax.inject.Inject

class SignOutUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    operator fun invoke() = repository.signOut()
}