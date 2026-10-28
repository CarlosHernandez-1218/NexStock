package com.dsmg11.nexstock.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
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
    // busca OTRO producto que ya tenga ese código de barras
    @Query("SELECT code FROM products WHERE barcode = :barcode AND code != :exceptCode LIMIT 1")
    suspend fun findBarcodeOwner(barcode: String, exceptCode: String): String?

    @Upsert
    suspend fun upsert(product: ProductEntity)

    @Upsert
    suspend fun upsertAll(products: List<ProductEntity>)

    // --- Sincronización (MOV-30) ---

    @Query("SELECT * FROM products WHERE pendingSync = 1")
    suspend fun getPending(): List<ProductEntity>

    @Query("SELECT code FROM products WHERE pendingSync = 1")
    suspend fun getPendingCodes(): List<String>

    // Solo se desmarca si no lo volvieron a editar mientras se subía
    @Query("UPDATE products SET pendingSync = 0 WHERE code = :code AND updatedAt = :updatedAt")
    suspend fun markSynced(code: String, updatedAt: Long)

    // Lo que llega de Firestore no pisa los cambios locales que aún no se suben
    @Transaction
    suspend fun upsertFromRemote(products: List<ProductEntity>) {
        val pending = getPendingCodes().toSet()
        upsertAll(products.filter { it.code !in pending })
    }

    // --- Recepción (HU07) ---
    // Suma lo recibido al stock local. NO marca pendingSync: en Firestore el stock
    // lo sube ReceptionSyncWorker con FieldValue.increment, así no se cuenta dos veces.
    @Query("UPDATE products SET stock = stock + :quantity WHERE code = :code")
    suspend fun addStock(code: String, quantity: Int)
}
