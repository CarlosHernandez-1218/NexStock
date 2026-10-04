package com.dsmg11.nexstock.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    @Query("SELECT * FROM products ORDER BY name ASC")
    fun observeAll(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE code = :code LIMIT 1")
    fun observeByCode(code: String): Flow<ProductEntity?>

    @Query("SELECT * FROM products WHERE code = :code LIMIT 1")
    suspend fun getByCode(code: String): ProductEntity?

    @Upsert
    suspend fun upsert(product: ProductEntity)

    @Upsert
    suspend fun upsertAll(products: List<ProductEntity>)
}