package com.dsmg11.nexstock.domain.model

enum class DeadlineStatus { VENCIDA, POR_VENCER, EN_PLAZO }

private const val DAY_MILLIS = 24L * 60 * 60 * 1000
const val DEADLINE_WARNING_DAYS = 3

// Días que faltan para la entrega máxima (negativo si ya venció).
// deliveryDate y todayUtc están ambos en medianoche UTC, por eso la resta sale en días exactos.
fun PurchaseOrder.daysUntilDelivery(todayUtc: Long = todayUtcMillis()): Int =
    ((deliveryDate - todayUtc) / DAY_MILLIS).toInt()

fun PurchaseOrder.deadlineStatus(todayUtc: Long = todayUtcMillis()): DeadlineStatus {
    val days = daysUntilDelivery(todayUtc)
    return when {
        days < 0 -> DeadlineStatus.VENCIDA
        days <= DEADLINE_WARNING_DAYS -> DeadlineStatus.POR_VENCER
        else -> DeadlineStatus.EN_PLAZO
    }
}

// Texto para mostrar en la tarjeta
fun PurchaseOrder.deadlineText(todayUtc: Long = todayUtcMillis()): String {
    val days = daysUntilDelivery(todayUtc)
    return when {
        days < 0 -> if (days == -1) "Vencida hace 1 día" else "Vencida hace ${-days} días"
        days == 0 -> "Vence hoy"
        days == 1 -> "Vence mañana"
        else -> "Vence en $days días"
    }
}