package com.dsmg11.nexstock.domain.usecase.dashboard

import com.dsmg11.nexstock.domain.model.DashboardSummary
import com.dsmg11.nexstock.domain.model.DayCount
import com.dsmg11.nexstock.domain.model.DeadlineStatus
import com.dsmg11.nexstock.domain.model.OrderStatus
import com.dsmg11.nexstock.domain.model.ProductStatus
import com.dsmg11.nexstock.domain.model.RotationLevel
import com.dsmg11.nexstock.domain.model.StockLevel
import com.dsmg11.nexstock.domain.model.deadlineStatus
import com.dsmg11.nexstock.domain.model.stockLevel
import com.dsmg11.nexstock.domain.repository.DashboardRepository
import com.dsmg11.nexstock.domain.repository.ProductRepository
import com.dsmg11.nexstock.domain.repository.PurchaseOrderRepository
import com.dsmg11.nexstock.domain.repository.ReceptionRepository
import com.dsmg11.nexstock.domain.usecase.replenishment.PendingOrdersReport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

// HU10: calcula KPIs y datos de los gráficos en tiempo real
class ObserveDashboardSummaryUseCase @Inject constructor(
    private val productRepository: ProductRepository,
    private val orderRepository: PurchaseOrderRepository,
    private val dashboardRepository: DashboardRepository,
    private val receptionRepository: ReceptionRepository
) {
    operator fun invoke(): Flow<DashboardSummary> {
        val days = lastDays(DAYS)
        // Recepciones ya subidas + las guardadas sin conexión que aún esperan sincronizarse
        val receptionTimes = combine(
            dashboardRepository.observeReceptionTimesSince(days.first()),
            receptionRepository.observePending().catch { emit(emptyList()) }
        ) { synced, pending -> synced + pending.map { it.receivedAt } }

        return combine(
            productRepository.observeProducts(),
            orderRepository.observeOrders().catch { emit(emptyList()) },
            dashboardRepository.observeReturnsToPickup(),
            receptionTimes
        ) { products, orders, returnsToPickup, times ->
            val active = products.filter { it.status == ProductStatus.ACTIVO }
            val pending = orders.filter(PendingOrdersReport::isPending)
            DashboardSummary(
                criticalStock = active.count { it.stockLevel == StockLevel.BAJO },
                outOfStock = active.count { it.stockLevel == StockLevel.AGOTADO },
                activeProducts = active.size,
                pendingOrders = pending.size,
                overdueOrders = pending.count { it.deadlineStatus() == DeadlineStatus.VENCIDA },
                returnsToPickup = returnsToPickup,
                ordersByStatus = OrderStatus.entries.associateWith { status -> orders.count { it.status == status } },
                receptionsByDay = days.map { start ->
                    val end = start + DAY_MILLIS
                    DayCount(label = dayLabel(start), count = times.count { it in start until end })
                },
                productsByRotation = RotationLevel.entries.associateWith { level -> active.count { it.rotation == level } }
            )
        }
    }

    // Inicio (medianoche local) de cada uno de los últimos n días, del más antiguo a hoy
    private fun lastDays(n: Int): List<Long> {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        return (n - 1 downTo 0).map { back ->
            (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -back) }.timeInMillis
        }
    }

    // Ej. "Lu\n3": día de la semana abreviado y, debajo, el número del día
    private fun dayLabel(startMillis: Long): String {
        val date = Date(startMillis)
        val weekday = SimpleDateFormat("EEE", SPANISH).format(date).take(2)
            .replaceFirstChar { it.uppercase() }
        val day = SimpleDateFormat("d", SPANISH).format(date)
        return "$weekday\n$day"
    }

    private companion object {
        const val DAYS = 7
        const val DAY_MILLIS = 24L * 60 * 60 * 1000
        val SPANISH = Locale("es", "PE")
    }
}
