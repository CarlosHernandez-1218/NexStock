package com.dsmg11.nexstock.domain.repository

import com.dsmg11.nexstock.domain.model.SupplierReturn
import kotlinx.coroutines.flow.Flow

interface SupplierReturnRepository {
    fun observeReturns(): Flow<List<SupplierReturn>>
    suspend fun register(supplierReturn: SupplierReturn): Result<Unit>
}