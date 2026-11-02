package com.dsmg11.nexstock.domain.usecase.reception

import com.dsmg11.nexstock.domain.model.PurchaseOrder
import com.dsmg11.nexstock.domain.model.withLocalReceptions
import com.dsmg11.nexstock.domain.repository.PurchaseOrderRepository
import com.dsmg11.nexstock.domain.repository.ReceptionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

// Una orden en tiempo real, sumando lo recibido sin conexión (null si no existe o no se puede ver)
class ObserveOrderUseCase @Inject constructor(
    private val orderRepository: PurchaseOrderRepository,
    private val receptionRepository: ReceptionRepository
) {
    operator fun invoke(orderId: String): Flow<PurchaseOrder?> =
        combine(orderRepository.observeOrders(), receptionRepository.observePending()) { orders, pending ->
            orders.firstOrNull { it.id == orderId }?.withLocalReceptions(pending)
        }
}
