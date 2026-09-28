package com.dsmg11.nexstock.domain.usecase.auth

import com.dsmg11.nexstock.domain.model.User
import com.dsmg11.nexstock.domain.repository.UserProfileRepository
import javax.inject.Inject

class EnsureUserProfileUseCase @Inject constructor(
    private val profileRepository: UserProfileRepository
) {
    suspend operator fun invoke(user: User): Result<Unit> =
        profileRepository.createProfileIfMissing(user.uid, user.email.orEmpty())
}