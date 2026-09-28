package com.dsmg11.nexstock.domain.usecase.profile

import com.dsmg11.nexstock.domain.model.AppFeature
import com.dsmg11.nexstock.domain.model.UserRole
import javax.inject.Inject

class GetAvailableFeaturesUseCase @Inject constructor() {
    operator fun invoke(role: UserRole): List<AppFeature> =
        AppFeature.entries.filter { role in it.allowedRoles }
}