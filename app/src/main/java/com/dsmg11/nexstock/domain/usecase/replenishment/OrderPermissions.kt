package com.dsmg11.nexstock.domain.usecase.replenishment

import com.dsmg11.nexstock.domain.model.OrderStatus
import com.dsmg11.nexstock.domain.model.PurchaseOrder
import com.dsmg11.nexstock.domain.model.UserProfile
import com.dsmg11.nexstock.domain.model.UserRole

object OrderPermissions {

    // Administrador: todas las órdenes. Jefe de Línea: las de sus líneas y las que él creó.
    fun visibleOrders(profile: UserProfile?, orders: List<PurchaseOrder>): List<PurchaseOrder> {
        if (profile == null) return emptyList()
        return when (profile.role) {
            UserRole.ADMINISTRADOR -> orders
            UserRole.JEFE_LINEA -> {
                val lines = profile.assignedLines.map { it.name }.toSet()
                orders.filter { order ->
                    order.createdBy == profile.uid || order.items.any { it.line in lines }
                }
            }
            UserRole.ALMACENERO -> emptyList()
        }
    }

    // Estados a los que este usuario puede llevar la orden desde la app.
    // Parcialmente recibida y Recibida las fija la recepción de mercadería (HU07), no se ofrecen aquí.
    fun availableTransitions(profile: UserProfile?, order: PurchaseOrder): List<OrderStatus> {
        if (profile == null) return emptyList()
        val manual = order.status.nextStates -
                setOf(OrderStatus.PARCIALMENTE_RECIBIDA, OrderStatus.RECIBIDA)
        return when (profile.role) {
            UserRole.ADMINISTRADOR -> manual.sortedBy { it.ordinal }
            // El Jefe de Línea solo puede cancelar sus propias órdenes mientras estén Generadas
            UserRole.JEFE_LINEA ->
                if (order.status == OrderStatus.GENERADA && order.createdBy == profile.uid)
                    listOf(OrderStatus.CANCELADA)
                else emptyList()
            UserRole.ALMACENERO -> emptyList()
        }
    }
}