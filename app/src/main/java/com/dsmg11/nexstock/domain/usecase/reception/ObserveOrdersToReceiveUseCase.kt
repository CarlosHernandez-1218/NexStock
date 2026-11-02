package com.dsmg11.nexstock.domain.usecase.reception

import com.dsmg11.nexstock.domain.model.PurchaseOrder
import com.dsmg11.nexstock.domain.model.isPendingReception
import com.dsmg11.nexstock.domain.repository.PurchaseOrderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

// MOV-47: órdenes pendientes de recepción, la de entrega más próxima primero
class ObserveOrdersToReceiveUseCase @Inject constructor(
    private val repository: PurchaseOrderRepository
) {
    operator fun invoke(): Flow<List<PurchaseOrder>> =
        repository.observeOrders().map { orders ->
            orders.filter { it.isPendingReception }.sortedBy { it.deliveryDate }
        }
}
