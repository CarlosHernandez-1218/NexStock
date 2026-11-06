package com.dsmg11.nexstock.domain.model

enum class ReturnType(val displayName: String) {
    CONSIGNACION("Stock consignación"),
    REPARACION("Reparación"),
    FALLADOS("Fallados")
}

enum class ReturnStatus(val displayName: String) {
    REGISTRADA("Registrada"),
    AUTORIZADA("Autorizada"),
    PENDIENTE_RECOJO("Pendiente de recojo"),
    RECOGIDA("Recogida");

    // El flujo es lineal: cada estado solo puede avanzar al siguiente
    val next: ReturnStatus? get() = entries.getOrNull(ordinal + 1)
    fun canMoveTo(target: ReturnStatus): Boolean = target == next
}

// Documento devoluciones/{id} en Firestore.
// Ojo: sin propiedades calculadas aquí, Firestore las guardaría como campos.
// Los cálculos van como funciones de extensión (igual que receivedPercent en PurchaseOrder).
data class SupplierReturn(
    val id: String = "",
    val productCode: String = "",
    val productName: String = "",
    val line: String = "",              // código de la línea (ej. PISOS)
    val unit: String = "",
    val quantity: Int = 0,
    val supplierRuc: String = "",
    val supplierName: String = "",
    val type: ReturnType = ReturnType.FALLADOS,
    val status: ReturnStatus = ReturnStatus.REGISTRADA,
    val createdBy: String = "",
    val createdByName: String = "",
    val createdAt: Long = 0L,
    // Los llenan las partes siguientes (MOV-60, 61 y 64)
    val authorizedBy: String = "",
    val authorizedByName: String = "",
    val authorizedAt: Long = 0L,
    val pickupRequestedAt: Long = 0L,
    val pickedUpAt: Long = 0L
)