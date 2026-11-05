package com.dsmg11.nexstock.domain.usecase.dashboard

import com.dsmg11.nexstock.domain.model.UserRole
import com.dsmg11.nexstock.domain.repository.PushRepository
import javax.inject.Inject

// MOV-70: solo el Administrador recibe el push de stock crítico y órdenes vencidas
class SyncAdminAlertsUseCase @Inject constructor(
    private val repository: PushRepository
) {
    operator fun invoke(role: UserRole?) = repository.setAdminAlerts(role == UserRole.ADMINISTRADOR)
}
