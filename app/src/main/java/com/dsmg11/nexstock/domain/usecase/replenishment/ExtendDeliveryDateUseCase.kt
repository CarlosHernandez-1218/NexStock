package com.dsmg11.nexstock.domain.usecase.replenishment

import com.dsmg11.nexstock.domain.model.OrderExtension
import com.dsmg11.nexstock.domain.model.PurchaseOrder
import com.dsmg11.nexstock.domain.model.UserProfile
import com.dsmg11.nexstock.domain.model.todayUtcMillis
import com.dsmg11.nexstock.domain.repository.PurchaseOrderRepository
import javax.inject.Inject

class ExtendDeliveryDateUseCase @Inject constructor(
    private val repository: PurchaseOrderRepository
) {
    suspend operator fun invoke(
        profile: UserProfile?,
        order: PurchaseOrder,
        newDate: Long,
        reason: String
    ): Result<Unit> {
        if (profile == null || !PendingOrdersReport.canExtend(profile, order)) {
            return Result.failure(IllegalStateException("No puedes cambiar la fecha de esta orden"))
        }
        if (reason.isBlank()) {
            return Result.failure(IllegalArgumentException("Escribe el motivo de la prórroga"))
        }
        if (newDate < todayUtcMillis()) {
            return Result.failure(IllegalArgumentException("La fecha no puede ser anterior a hoy"))
        }
        if (newDate == order.deliveryDate) {
            return Result.failure(IllegalArgumentException("Elige una fecha distinta a la actual"))
        }
        return repository.extendDeadline(
            orderId = order.id,
            extension = OrderExtension(
                orderId = order.id,
                supplierName = order.supplierName,
                newDate = newDate,
                reason = reason.trim(),
                requestedBy = profile.uid,
                requestedByName = profile.fullName.ifBlank { profile.email },
                createdAt = System.currentTimeMillis()
            )
        )
    }
}