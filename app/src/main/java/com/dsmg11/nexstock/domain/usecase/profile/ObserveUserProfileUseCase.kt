package com.dsmg11.nexstock.domain.usecase.profile

import com.dsmg11.nexstock.domain.model.UserProfile
import com.dsmg11.nexstock.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveUserProfileUseCase @Inject constructor(
    private val repository: UserProfileRepository
) {
    operator fun invoke(uid: String): Flow<UserProfile?> = repository.observeProfile(uid)
}