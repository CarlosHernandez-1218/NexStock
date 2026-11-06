package com.dsmg11.nexstock.domain.repository

import com.dsmg11.nexstock.domain.model.SupplierReturn
import kotlinx.coroutines.flow.Flow
import com.dsmg11.nexstock.domain.model.ReturnStatus

interface SupplierReturnRepository {
    fun observeReturns(): Flow<List<SupplierReturn>>
    suspend fun register(supplierReturn: SupplierReturn): Result<Unit>
    // Cambia el estado validando el flujo (cada estado solo avanza al siguiente) en una transacción
    suspend fun moveTo(returnId: String, target: ReturnStatus, fields: Map<String, Any>): Result<Unit>
    // Confirma el recojo: pasa a Recogida y descuenta el stock del producto todo en una transacción
    suspend fun confirmPickup(returnId: String): Result<Unit>
}