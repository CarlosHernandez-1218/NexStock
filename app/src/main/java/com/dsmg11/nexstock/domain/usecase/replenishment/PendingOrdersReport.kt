package com.dsmg11.nexstock.domain.usecase.replenishment

import com.dsmg11.nexstock.domain.model.PurchaseOrder
import com.dsmg11.nexstock.domain.model.UserProfile
import com.dsmg11.nexstock.domain.model.UserRole

object PendingOrdersReport {

    // Pendiente = no está en un estado final (Recibida o Cancelada)
    fun isPending(order: PurchaseOrder): Boolean = !order.status.isFinal

    // Jefe de Línea: solo órdenes con algún producto de SUS líneas.
    // Administrador: todas. Almacenero: ninguna.
    // La más urgente (fecha de entrega más cercana) va primero.
    fun build(profile: UserProfile?, orders: List<PurchaseOrder>): List<PurchaseOrder> {
        if (profile == null) return emptyList()
        val pending = orders.filter(::isPending)
        val visible = when (profile.role) {
            UserRole.ADMINISTRADOR -> pending
            UserRole.JEFE_LINEA -> {
                val lines = profile.assignedLines.map { it.name }.toSet()
                pending.filter { order -> order.items.any { it.line in lines } }
            }
            UserRole.ALMACENERO -> emptyList()
        }
        return visible.sortedBy { it.deliveryDate }
    }

    fun canExtend(profile: UserProfile?, order: PurchaseOrder): Boolean {
        if (profile == null || profile.role != UserRole.JEFE_LINEA || !isPending(order)) return false
        val lines = profile.assignedLines.map { it.name }.toSet()
        return order.items.any { it.line in lines }
    }
}