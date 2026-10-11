package com.dsmg11.nexstock.domain.model

data class PurchaseOrderItem(
    val code: String = "",
    val name: String = "",
    val unit: String = "",
    val line: String = "",                   // código de la línea (ej. PISOS)
    val quantity: Int = 0
)

data class PurchaseOrder(
    val id: String = "",
    val items: List<PurchaseOrderItem> = emptyList(),
    val supplierRuc: String = "",
    val supplierName: String = "",
    val deliveryDate: Long = 0L,             // fecha máxima pactada (medianoche UTC, en milisegundos)
    val status: OrderStatus = OrderStatus.GENERADA,
    val createdBy: String = "",              // uid del usuario que la generó
    val createdAt: Long = 0L
)