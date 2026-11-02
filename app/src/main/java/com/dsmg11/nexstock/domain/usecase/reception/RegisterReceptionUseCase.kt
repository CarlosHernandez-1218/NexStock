package com.dsmg11.nexstock.domain.usecase.reception

import com.dsmg11.nexstock.domain.model.PurchaseOrder
import com.dsmg11.nexstock.domain.model.Reception
import com.dsmg11.nexstock.domain.model.ReceptionItem
import com.dsmg11.nexstock.domain.model.isPendingReception
import com.dsmg11.nexstock.domain.model.pendingOf
import com.dsmg11.nexstock.domain.repository.ReceptionRepository
import com.dsmg11.nexstock.domain.usecase.GetCurrentUserUseCase
import java.util.UUID
import javax.inject.Inject

// MOV-50: arma la recepción con lo que llegó y la guarda (también sin internet)
class RegisterReceptionUseCase @Inject constructor(
    private val repository: ReceptionRepository,
    private val getCurrentUser: GetCurrentUserUseCase
) {
    suspend operator fun invoke(order: PurchaseOrder, quantities: Map<String, Int>): Result<Reception> {
        val user = getCurrentUser()
            ?: return Result.failure(IllegalStateException("Inicia sesión para registrar la recepción"))
        if (!order.isPendingReception) {
            return Result.failure(IllegalStateException("Esta orden ya no admite recepciones"))
        }

        val items = order.items.mapNotNull { item ->
            val qty = quantities[item.code] ?: 0
            val pending = order.pendingOf(item)
            when {
                qty <= 0 -> null
                qty > pending -> return Result.failure(
                    IllegalArgumentException("${item.name}: solo faltan $pending ${item.unit}")
                )
                else -> ReceptionItem(
                    code = item.code,
                    name = item.name,
                    unit = item.unit,
                    line = item.line,
                    ordered = item.quantity,
                    received = qty
                )
            }
        }
        if (items.isEmpty()) return Result.failure(IllegalArgumentException("Ingresa al menos una cantidad"))

        val reception = Reception(
            id = UUID.randomUUID().toString(),   // el id nace en el celular: así subirla dos veces no duplica
            orderId = order.id,
            supplierRuc = order.supplierRuc,
            supplierName = order.supplierName,
            items = items,
            receivedBy = user.uid,
            receivedAt = System.currentTimeMillis(),
            location = null,                     // HU08 (Mauro) agregará la ubicación GPS
            pendingSync = true
        )
        return repository.register(reception).map { reception }
    }
}
