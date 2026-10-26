package com.dsmg11.nexstock.domain.repository

import com.dsmg11.nexstock.domain.model.OrderStatus
import com.dsmg11.nexstock.domain.model.PurchaseOrder
import kotlinx.coroutines.flow.Flow
import com.dsmg11.nexstock.domain.model.OrderSchedule

interface PurchaseOrderRepository {
    // Devuelve el id de la orden creada
    suspend fun createOrder(order: PurchaseOrder): Result<String>
    // Cambia el estado dentro de una transacción, validando la transición con el estado actual
    suspend fun updateStatus(orderId: String, status: OrderStatus): Result<Unit>
    // Todas las órdenes en tiempo real, las más recientes primero
    suspend fun consolidate(sourceIds: List<String>, merged: PurchaseOrder): Result<String>
    // Cronogramas de todas las líneas, en tiempo real
    fun observeSchedules(): Flow<List<OrderSchedule>>
    // Crea o reemplaza el cronograma de una línea
    suspend fun saveSchedule(schedule: OrderSchedule): Result<Unit>
    fun observeOrders(): Flow<List<PurchaseOrder>>
}