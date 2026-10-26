package com.dsmg11.nexstock.domain.usecase.replenishment

import com.dsmg11.nexstock.domain.model.OrderSchedule
import com.dsmg11.nexstock.domain.model.ScheduleFrequency
import com.dsmg11.nexstock.domain.model.todayUtcMillis
import com.dsmg11.nexstock.domain.repository.PurchaseOrderRepository
import com.dsmg11.nexstock.domain.usecase.GetCurrentUserUseCase
import javax.inject.Inject

class SaveOrderScheduleUseCase @Inject constructor(
    private val repository: PurchaseOrderRepository,
    private val getCurrentUser: GetCurrentUserUseCase
) {
    suspend operator fun invoke(
        lineCode: String,
        frequency: ScheduleFrequency,
        nextDate: Long
    ): Result<Unit> {
        if (lineCode.isBlank()) {
            return Result.failure(IllegalArgumentException("Elige una línea"))
        }
        if (nextDate < todayUtcMillis()) {
            return Result.failure(IllegalArgumentException("La fecha no puede ser anterior a hoy"))
        }
        val user = getCurrentUser()
            ?: return Result.failure(IllegalStateException("No hay una sesión iniciada"))

        return repository.saveSchedule(
            OrderSchedule(
                lineCode = lineCode,
                frequency = frequency,
                nextDate = nextDate,
                updatedBy = user.uid,
                updatedAt = System.currentTimeMillis()
            )
        )
    }
}