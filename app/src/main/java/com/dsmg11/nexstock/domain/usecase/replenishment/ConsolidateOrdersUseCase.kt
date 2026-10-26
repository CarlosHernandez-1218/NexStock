package com.dsmg11.nexstock.domain.usecase.replenishment

import com.dsmg11.nexstock.domain.model.OrderStatus
import com.dsmg11.nexstock.domain.model.PurchaseOrder
import com.dsmg11.nexstock.domain.repository.PurchaseOrderRepository
import com.dsmg11.nexstock.domain.usecase.GetCurrentUserUseCase
import javax.inject.Inject

class ConsolidateOrdersUseCase @Inject constructor(
    private val repository: PurchaseOrderRepository,
    private val getCurrentUser: GetCurrentUserUseCase
) {
    suspend operator fun invoke(orders: List<PurchaseOrder>): Result<String> {
        if (orders.size < 2) {
            return Result.failure(IllegalArgumentException("Elige al menos dos órdenes"))
        }
        if (orders.any { it.status != OrderStatus.GENERADA }) {
            return Result.failure(IllegalArgumentException("Solo se pueden consolidar órdenes Generadas"))
        }
        if (orders.map { it.supplierRuc }.distinct().size != 1) {
            return Result.failure(IllegalArgumentException("Las órdenes deben ser del mismo proveedor"))
        }
        val user = getCurrentUser()
            ?: return Result.failure(IllegalStateException("No hay una sesión iniciada"))

        // Mismo producto en varias órdenes: se junta en una sola línea sumando cantidades
        val items = orders.flatMap { it.items }
            .groupBy { it.code }
            .map { (_, group) -> group.first().copy(quantity = group.sumOf { it.quantity }) }

        val first = orders.first()
        val merged = PurchaseOrder(
            items = items,
            supplierRuc = first.supplierRuc,
            supplierName = first.supplierName,
            deliveryDate = orders.minOf { it.deliveryDate },   // la fecha más exigente
            status = OrderStatus.GENERADA,
            createdBy = user.uid,
            createdAt = System.currentTimeMillis(),
            consolidatedFrom = orders.map { it.id }
        )
        return repository.consolidate(orders.map { it.id }, merged)
    }
}