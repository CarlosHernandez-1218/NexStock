package com.dsmg11.nexstock.domain.usecase.reception

import com.dsmg11.nexstock.domain.model.PurchaseOrder
import com.dsmg11.nexstock.domain.repository.PurchaseOrderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

// Una orden en tiempo real (null si no existe o el usuario no puede verla)
class ObserveOrderUseCase @Inject constructor(
    private val repository: PurchaseOrderRepository
) {
    operator fun invoke(orderId: String): Flow<PurchaseOrder?> =
        repository.observeOrders().map { orders -> orders.firstOrNull { it.id == orderId } }
}
