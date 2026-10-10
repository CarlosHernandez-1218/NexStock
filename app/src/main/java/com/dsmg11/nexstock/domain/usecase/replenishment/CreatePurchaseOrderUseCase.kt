package com.dsmg11.nexstock.domain.usecase.replenishment

import com.dsmg11.nexstock.domain.model.OrderStatus
import com.dsmg11.nexstock.domain.model.PurchaseOrder
import com.dsmg11.nexstock.domain.model.PurchaseOrderItem
import com.dsmg11.nexstock.domain.model.Supplier
import com.dsmg11.nexstock.domain.repository.PurchaseOrderRepository
import com.dsmg11.nexstock.domain.usecase.GetCurrentUserUseCase
import javax.inject.Inject

class CreatePurchaseOrderUseCase @Inject constructor(
    private val repository: PurchaseOrderRepository,
    private val getCurrentUser: GetCurrentUserUseCase
) {
    // Toda orden nueva nace en estado GENERADA
    suspend operator fun invoke(
        items: List<PurchaseOrderItem>,
        supplier: Supplier,
        deliveryDate: Long
    ): Result<String> {
        if (items.isEmpty() || items.any { it.quantity <= 0 }) {
            return Result.failure(IllegalArgumentException("Revisa los productos y sus cantidades"))
        }
        val user = getCurrentUser()
            ?: return Result.failure(IllegalStateException("No hay una sesión iniciada"))

        val order = PurchaseOrder(
            items = items,
            supplierRuc = supplier.ruc,
            supplierName = supplier.razonSocial,
            deliveryDate = deliveryDate,
            status = OrderStatus.GENERADA,
            createdBy = user.uid,
            createdAt = System.currentTimeMillis()
        )
        return repository.createOrder(order)
    }
}