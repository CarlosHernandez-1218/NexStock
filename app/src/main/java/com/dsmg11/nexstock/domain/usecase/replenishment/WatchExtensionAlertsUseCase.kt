package com.dsmg11.nexstock.domain.usecase.replenishment

import com.dsmg11.nexstock.domain.repository.ExtensionAlertRepository
import com.dsmg11.nexstock.domain.repository.PurchaseOrderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class WatchExtensionAlertsUseCase @Inject constructor(
    private val orders: PurchaseOrderRepository,
    private val alerts: ExtensionAlertRepository
) {
    // Avisa de las prórrogas nuevas. La primera vez en este celular solo se empieza a contar
    // desde ahora, para no avisar de prórrogas viejas.
    operator fun invoke(): Flow<Unit> {
        val since = alerts.lastSeen().takeIf { it > 0 }
            ?: System.currentTimeMillis().also { alerts.saveLastSeen(it) }

        return orders.observeExtensionsSince(since).map { list ->
            val fresh = list.filter { it.createdAt > alerts.lastSeen() }
            if (fresh.isNotEmpty() && alerts.showAlert(fresh)) {
                alerts.saveLastSeen(fresh.maxOf { it.createdAt })
            }
        }
    }
}