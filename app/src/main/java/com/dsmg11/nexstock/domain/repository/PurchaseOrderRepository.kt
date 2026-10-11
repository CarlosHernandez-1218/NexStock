package com.dsmg11.nexstock.domain.repository

import com.dsmg11.nexstock.domain.model.OrderStatus
import com.dsmg11.nexstock.domain.model.PurchaseOrder
import kotlinx.coroutines.flow.Flow

interface PurchaseOrderRepository {
    // Devuelve el id de la orden creada
    suspend fun createOrder(order: PurchaseOrder): Result<String>
    // Cambia el estado dentro de una transacción, validando la transición con el estado actual
    suspend fun updateStatus(orderId: String, status: OrderStatus): Result<Unit>
    // Todas las órdenes en tiempo real, las más recientes primero
    fun observeOrders(): Flow<List<PurchaseOrder>>
}