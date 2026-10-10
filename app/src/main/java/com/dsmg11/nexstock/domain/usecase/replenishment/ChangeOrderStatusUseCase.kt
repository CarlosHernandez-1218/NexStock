package com.dsmg11.nexstock.domain.usecase.replenishment

import com.dsmg11.nexstock.domain.model.OrderStatus
import com.dsmg11.nexstock.domain.model.PurchaseOrder
import com.dsmg11.nexstock.domain.repository.PurchaseOrderRepository
import javax.inject.Inject

class ChangeOrderStatusUseCase @Inject constructor(
    private val repository: PurchaseOrderRepository
) {
    // Solo permite las transiciones definidas en OrderStatus
    suspend operator fun invoke(order: PurchaseOrder, next: OrderStatus): Result<Unit> {
        if (!order.status.canChangeTo(next)) {
            return Result.failure(
                IllegalStateException(
                    "No se puede pasar de ${order.status.displayName} a ${next.displayName}"
                )
            )
        }
        return repository.updateStatus(order.id, next)
    }
}