package com.dsmg11.nexstock.domain.model

enum class OrderStatus(val displayName: String) {
    GENERADA("Generada"),
    APROBADA("Aprobada"),
    ENVIADA("Enviada"),
    PARCIALMENTE_RECIBIDA("Parcialmente recibida"),
    RECIBIDA("Recibida"),
    CANCELADA("Cancelada");

    // Estados a los que se puede pasar desde este
    val nextStates: Set<OrderStatus>
        get() = when (this) {
            GENERADA -> setOf(APROBADA, CANCELADA)
            APROBADA -> setOf(ENVIADA, CANCELADA)
            ENVIADA -> setOf(PARCIALMENTE_RECIBIDA, RECIBIDA, CANCELADA)
            PARCIALMENTE_RECIBIDA -> setOf(RECIBIDA)
            RECIBIDA, CANCELADA -> emptySet()
        }

    fun canChangeTo(next: OrderStatus): Boolean = next in nextStates

    val isFinal: Boolean get() = nextStates.isEmpty()
}