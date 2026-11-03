package com.dsmg11.nexstock.domain.model

// HU10 - MOV-66: indicadores del dashboard del Administrador
data class DashboardSummary(
    val criticalStock: Int = 0,      // productos activos en o bajo su stock mínimo (sin estar agotados)
    val outOfStock: Int = 0,         // productos activos sin existencias
    val activeProducts: Int = 0,
    val pendingOrders: Int = 0,      // órdenes que no están Recibidas ni Canceladas
    val overdueOrders: Int = 0,      // de las pendientes, las que pasaron su fecha máxima de entrega
    val returnsToPickup: Int = 0     // HU09: devoluciones en estado Pendiente de recojo
)
