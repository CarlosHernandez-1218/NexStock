package com.dsmg11.nexstock.domain.usecase.replenishment

import com.dsmg11.nexstock.domain.model.PurchaseOrder
import com.dsmg11.nexstock.domain.repository.PurchaseOrderRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveOrdersUseCase @Inject constructor(
    private val repository: PurchaseOrderRepository
) {
    operator fun invoke(): Flow<List<PurchaseOrder>> = repository.observeOrders()
}