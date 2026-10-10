package com.dsmg11.nexstock.domain.repository

import com.dsmg11.nexstock.domain.model.OrderStatus
import com.dsmg11.nexstock.domain.model.PurchaseOrder

interface PurchaseOrderRepository {
    // Devuelve el id de la orden creada
    suspend fun createOrder(order: PurchaseOrder): Result<String>
    suspend fun updateStatus(orderId: String, status: OrderStatus): Result<Unit>
}