package com.dsmg11.nexstock.domain.model

// Registro de un cambio de fecha máxima de entrega (prórroga)
data class OrderExtension(
    val id: String = "",
    val orderId: String = "",
    val supplierName: String = "",
    val previousDate: Long = 0L,        // fecha anterior (medianoche UTC)
    val newDate: Long = 0L,             // fecha nueva (medianoche UTC)
    val reason: String = "",
    val requestedBy: String = "",       // uid del Jefe de Línea
    val requestedByName: String = "",
    val createdAt: Long = 0L
)