package com.dsmg11.nexstock.domain.usecase

import com.dsmg11.nexstock.domain.model.User
import com.dsmg11.nexstock.domain.repository.AuthRepository
import javax.inject.Inject

class GetCurrentUserUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    operator fun invoke(): User? = repository.currentUser
}