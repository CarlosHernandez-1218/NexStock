package com.dsmg11.nexstock.domain.usecase.supplier

import com.dsmg11.nexstock.domain.model.Supplier
import com.dsmg11.nexstock.domain.repository.SupplierRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveSuppliersUseCase @Inject constructor(
    private val repository: SupplierRepository
) {
    operator fun invoke(): Flow<List<Supplier>> = repository.observarProveedores()
}