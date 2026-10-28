package com.dsmg11.nexstock.domain.model

data class ReceptionItem(
    val code: String = "",
    val name: String = "",
    val unit: String = "",
    val line: String = "",
    val ordered: Int = 0,      // cantidad pedida en la orden
    val received: Int = 0      // cantidad que llegó en ESTA recepción
)

data class ReceptionLocation(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

data class Reception(
    val id: String = "",                       // UUID generado en el celular
    val orderId: String = "",
    val supplierRuc: String = "",
    val supplierName: String = "",
    val items: List<ReceptionItem> = emptyList(),
    val receivedBy: String = "",               // uid del almacenero
    val receivedAt: Long = 0L,
    val location: ReceptionLocation? = null,   // la llena la HU08 (geolocalización)
    val pendingSync: Boolean = false           // solo local: aún no se sube a Firestore
)
