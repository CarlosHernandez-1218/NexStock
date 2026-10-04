package com.dsmg11.nexstock.domain.repository

import com.dsmg11.nexstock.domain.model.Product
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    fun observeProducts(): Flow<List<Product>>
    fun observeProduct(code: String): Flow<Product?>
    suspend fun existsProduct(code: String): Boolean
    suspend fun saveProduct(product: Product): Result<Unit>
}