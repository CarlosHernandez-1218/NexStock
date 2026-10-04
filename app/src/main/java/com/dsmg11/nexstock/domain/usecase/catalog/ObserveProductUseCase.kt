package com.dsmg11.nexstock.domain.usecase.catalog

import com.dsmg11.nexstock.domain.model.Product
import com.dsmg11.nexstock.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveProductUseCase @Inject constructor(
    private val repository: ProductRepository
) {
    operator fun invoke(code: String): Flow<Product?> = repository.observeProduct(code)
}