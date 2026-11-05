package com.dsmg11.nexstock.domain.model

// HU10: datos del dashboard del Administrador
data class DashboardSummary(
    // MOV-66: KPIs
    val criticalStock: Int = 0,      // productos activos en o bajo su stock mínimo (sin estar agotados)
    val outOfStock: Int = 0,         // productos activos sin existencias
    val activeProducts: Int = 0,
    val pendingOrders: Int = 0,      // órdenes que no están Recibidas ni Canceladas
    val overdueOrders: Int = 0,      // de las pendientes, las que pasaron su fecha máxima de entrega
    val returnsToPickup: Int = 0,    // HU09: devoluciones en estado Pendiente de recojo
    // MOV-67: gráficos
    val ordersByStatus: Map<OrderStatus, Int> = emptyMap(),
    val receptionsByDay: List<DayCount> = emptyList(),   // últimos 7 días, del más antiguo a hoy
    // MOV-68: distribución por nivel de rotación (productos activos)
    val productsByRotation: Map<RotationLevel, Int> = emptyMap()
)

data class DayCount(val label: String, val count: Int)
