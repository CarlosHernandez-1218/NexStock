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
    val createdAt: Long = 0L,
    val consolidatedFrom: List<String> = emptyList(),  // ids de las órdenes que se juntaron en esta
    val consolidatedInto: String = "",                  // si tiene valor, esta orden se juntó en otra
    val received: Map<String, Int> = emptyMap()         // HU07: acumulado recibido por código de producto
)

// Cuánto falta recibir de un producto
fun PurchaseOrder.pendingOf(item: PurchaseOrderItem): Int =
    (item.quantity - (received[item.code] ?: 0)).coerceAtLeast(0)

// % recibido de la orden (lo usa Carlos en MOV-43)
val PurchaseOrder.receivedPercent: Int
    get() {
        val ordered = items.sumOf { it.quantity }
        if (ordered == 0) return 0
        val got = items.sumOf { minOf(it.quantity, received[it.code] ?: 0) }
        return got * 100 / ordered
    }
