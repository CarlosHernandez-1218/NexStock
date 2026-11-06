package com.dsmg11.nexstock.domain.usecase.returns

import com.dsmg11.nexstock.domain.model.SupplierReturn
import com.dsmg11.nexstock.domain.repository.SupplierReturnRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveReturnsUseCase @Inject constructor(
    private val repository: SupplierReturnRepository
) {
    operator fun invoke(): Flow<List<SupplierReturn>> = repository.observeReturns()
}