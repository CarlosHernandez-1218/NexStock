package com.dsmg11.nexstock.domain.usecase.returns

import com.dsmg11.nexstock.domain.model.ReturnStatus
import com.dsmg11.nexstock.domain.model.SupplierReturn
import com.dsmg11.nexstock.domain.model.UserProfile
import com.dsmg11.nexstock.domain.repository.SupplierReturnRepository
import javax.inject.Inject

// MOV-61: la devolución autorizada pasa a Pendiente de recojo (ya se coordina con el proveedor)
class RequestPickupUseCase @Inject constructor(
    private val repository: SupplierReturnRepository
) {
    suspend operator fun invoke(profile: UserProfile?, item: SupplierReturn): Result<Unit> {
        if (profile == null || !ReturnPermissions.canRequestPickup(profile, item)) {
            return Result.failure(
                IllegalStateException("Solo el Jefe de Línea responsable puede coordinar el recojo")
            )
        }
        return repository.moveTo(
            returnId = item.id,
            target = ReturnStatus.PENDIENTE_RECOJO,
            fields = mapOf("pickupRequestedAt" to System.currentTimeMillis())
        )
    }
}