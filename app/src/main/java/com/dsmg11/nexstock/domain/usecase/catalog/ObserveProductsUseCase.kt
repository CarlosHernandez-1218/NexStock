package com.dsmg11.nexstock.domain.usecase.catalog

import com.dsmg11.nexstock.domain.model.Product
import com.dsmg11.nexstock.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveProductsUseCase @Inject constructor(
    private val repository: ProductRepository
) {
    operator fun invoke(): Flow<List<Product>> = repository.observeProducts()
}