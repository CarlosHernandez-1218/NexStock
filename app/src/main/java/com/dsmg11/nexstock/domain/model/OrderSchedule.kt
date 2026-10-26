package com.dsmg11.nexstock.domain.model

import java.util.Calendar
import java.util.TimeZone

enum class ScheduleFrequency(val displayName: String, val days: Int) {
    SEMANAL("Semanal", 7),
    QUINCENAL("Quincenal", 15),
    MENSUAL("Mensual", 30)
}

// Cronograma de generación de órdenes de una línea. El id del documento es el código de la línea.
data class OrderSchedule(
    val lineCode: String = "",                          // código de la línea (ej. PISOS)
    val frequency: ScheduleFrequency = ScheduleFrequency.SEMANAL,
    val nextDate: Long = 0L,                            // próxima generación (medianoche UTC, en milisegundos)
    val updatedBy: String = "",                         // uid de quien lo definió
    val updatedAt: Long = 0L
)

enum class ScheduleStatus { VENCIDO, HOY, PROXIMO }

// Compara la fecha del cronograma con hoy (hoy también en medianoche UTC)
fun OrderSchedule.statusOn(todayUtc: Long): ScheduleStatus = when {
    nextDate < todayUtc -> ScheduleStatus.VENCIDO
    nextDate == todayUtc -> ScheduleStatus.HOY
    else -> ScheduleStatus.PROXIMO
}

// El DatePicker guarda las fechas como medianoche UTC. Esto da el día de hoy (hora local)
// en ese mismo formato, para poder compararlas sin errores de un día.
fun todayUtcMillis(): Long {
    val local = Calendar.getInstance()
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}