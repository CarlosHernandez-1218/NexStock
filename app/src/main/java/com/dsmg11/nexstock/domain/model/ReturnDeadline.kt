package com.dsmg11.nexstock.domain.model

const val RETURN_LIMIT_DAYS = 7       // días máximos de una devolución en almacén
const val RETURN_WARNING_DAYS = 5     // desde este día ya se alerta

enum class WarehouseAlert { NEAR_LIMIT, OVER_LIMIT }

private const val DAY_MILLIS = 24L * 60 * 60 * 1000

// Días que lleva en almacén (desde que se registró)
fun SupplierReturn.daysInWarehouse(now: Long = System.currentTimeMillis()): Int =
    ((now - createdAt) / DAY_MILLIS).toInt().coerceAtLeast(0)

// null si no hay nada que alertar (ya fue recogida o aún le sobra tiempo)
fun SupplierReturn.warehouseAlert(now: Long = System.currentTimeMillis()): WarehouseAlert? {
    if (status == ReturnStatus.RECOGIDA) return null
    val days = daysInWarehouse(now)
    return when {
        days >= RETURN_LIMIT_DAYS -> WarehouseAlert.OVER_LIMIT
        days >= RETURN_WARNING_DAYS -> WarehouseAlert.NEAR_LIMIT
        else -> null
    }
}

fun SupplierReturn.warehouseText(now: Long = System.currentTimeMillis()): String {
    val days = daysInWarehouse(now)
    val left = RETURN_LIMIT_DAYS - days
    return when {
        left > 1 -> "Lleva $days días en almacén (quedan $left)"
        left == 1 -> "Lleva $days días en almacén (queda 1)"
        left == 0 -> "Llegó al límite de $RETURN_LIMIT_DAYS días en almacén"
        else -> "Excede el límite: $days días en almacén"
    }
}