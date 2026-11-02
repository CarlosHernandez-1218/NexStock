package com.dsmg11.nexstock.domain.usecase.reception

import com.dsmg11.nexstock.domain.model.PurchaseOrder
import com.dsmg11.nexstock.domain.model.isPendingReception
import com.dsmg11.nexstock.domain.model.withLocalReceptions
import com.dsmg11.nexstock.domain.repository.PurchaseOrderRepository
import com.dsmg11.nexstock.domain.repository.ReceptionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

// MOV-47: órdenes pendientes de recepción, la de entrega más próxima primero.
// Incluye lo recibido sin conexión, así una orden completada offline ya no aparece.
class ObserveOrdersToReceiveUseCase @Inject constructor(
    private val orderRepository: PurchaseOrderRepository,
    private val receptionRepository: ReceptionRepository
) {
    operator fun invoke(): Flow<List<PurchaseOrder>> =
        combine(orderRepository.observeOrders(), receptionRepository.observePending()) { orders, pending ->
            orders.map { it.withLocalReceptions(pending) }
                .filter { it.isPendingReception }
                .sortedBy { it.deliveryDate }
        }
}
