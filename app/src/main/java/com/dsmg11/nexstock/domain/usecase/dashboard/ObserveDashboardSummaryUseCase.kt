package com.dsmg11.nexstock.domain.usecase.dashboard

import com.dsmg11.nexstock.domain.model.DashboardSummary
import com.dsmg11.nexstock.domain.model.DeadlineStatus
import com.dsmg11.nexstock.domain.model.ProductStatus
import com.dsmg11.nexstock.domain.model.StockLevel
import com.dsmg11.nexstock.domain.model.deadlineStatus
import com.dsmg11.nexstock.domain.model.stockLevel
import com.dsmg11.nexstock.domain.repository.DashboardRepository
import com.dsmg11.nexstock.domain.repository.ProductRepository
import com.dsmg11.nexstock.domain.repository.PurchaseOrderRepository
import com.dsmg11.nexstock.domain.usecase.replenishment.PendingOrdersReport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

// MOV-66: calcula los KPIs en tiempo real a partir de productos, órdenes y devoluciones
class ObserveDashboardSummaryUseCase @Inject constructor(
    private val productRepository: ProductRepository,
    private val orderRepository: PurchaseOrderRepository,
    private val dashboardRepository: DashboardRepository
) {
    operator fun invoke(): Flow<DashboardSummary> = combine(
        productRepository.observeProducts(),
        orderRepository.observeOrders().catch { emit(emptyList()) },
        dashboardRepository.observeReturnsToPickup()
    ) { products, orders, returnsToPickup ->
        val active = products.filter { it.status == ProductStatus.ACTIVO }
        val pending = orders.filter(PendingOrdersReport::isPending)
        DashboardSummary(
            criticalStock = active.count { it.stockLevel == StockLevel.BAJO },
            outOfStock = active.count { it.stockLevel == StockLevel.AGOTADO },
            activeProducts = active.size,
            pendingOrders = pending.size,
            overdueOrders = pending.count { it.deadlineStatus() == DeadlineStatus.VENCIDA },
            returnsToPickup = returnsToPickup
        )
    }
}
