package com.dsmg11.nexstock.domain.usecase.profile

import com.dsmg11.nexstock.domain.repository.UserProfileRepository
import javax.inject.Inject

class UpdateProfileUseCase @Inject constructor(
    private val repository: UserProfileRepository
) {
    suspend operator fun invoke(uid: String, fullName: String, phone: String): Result<Unit> {
        val name = fullName.trim()
        val cellphone = phone.trim()

        if (name.length < 3) {
            return Result.failure(IllegalArgumentException("Ingresa tu nombre completo"))
        }
        if (cellphone.isNotEmpty() && !PERU_CELLPHONE.matches(cellphone)) {
            return Result.failure(IllegalArgumentException("El celular debe tener 9 dígitos y empezar con 9"))
        }
        return repository.updateBasicInfo(uid, name, cellphone)
    }

    private companion object {
        val PERU_CELLPHONE = Regex("^9\\d{8}$")
    }
}