package com.dsmg11.nexstock.data.repository

import android.content.Context
import com.dsmg11.nexstock.data.local.ProductDao
import com.dsmg11.nexstock.data.local.toDomain
import com.dsmg11.nexstock.data.local.toEntity
import com.dsmg11.nexstock.data.sync.ProductSyncWorker
import com.dsmg11.nexstock.domain.model.MasterType
import com.dsmg11.nexstock.domain.model.Product
import com.dsmg11.nexstock.domain.model.ProductLine
import com.dsmg11.nexstock.domain.model.ProductStatus
import com.dsmg11.nexstock.domain.model.RotationLevel
import com.dsmg11.nexstock.domain.model.withName
import com.dsmg11.nexstock.domain.repository.MasterDataRepository
import com.dsmg11.nexstock.domain.repository.ProductRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

class ProductRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firestore: FirebaseFirestore,
    private val productDao: ProductDao,
    private val masterData: MasterDataRepository
) : ProductRepository {

    private val products get() = firestore.collection(PRODUCTS_COLLECTION)

    // Nombres de las líneas (código → nombre), leídos de la colección product_lines
    private val lineNames: Flow<Map<String, String>> =
        masterData.observe(MasterType.LINEAS)
            .map { items -> items.associate { it.id to it.name } }
            .onStart { emit(emptyMap()) }
            .catch { emit(emptyMap()) }

    // Offline-first: la UI lee de Room; Firestore solo mantiene Room actualizado
    override fun observeProducts(): Flow<List<Product>> {
        val local = channelFlow {
            launch {
                remoteProducts()
                    .catch { /* Sin conexión: se sigue mostrando la copia local */ }
                    .collect { list -> productDao.upsertFromRemote(list.map { it.toEntity() }) }
            }
            productDao.observeAll()
                .map { list -> list.map { it.toDomain() } }
                .collect { send(it) }
        }
        return combine(local, lineNames) { list, names ->
            list.map { it.copy(line = it.line.withName(names)) }
        }
    }

    override fun observeProduct(code: String): Flow<Product?> =
        combine(productDao.observeByCode(code), lineNames) { entity, names ->
            entity?.toDomain()?.let { it.copy(line = it.line.withName(names)) }
        }

    override suspend fun existsProduct(code: String): Boolean =
        productDao.getByCode(code) != null

    override suspend fun saveProduct(product: Product): Result<Unit> = runCatching {
        val stamped = product.copy(updatedAt = System.currentTimeMillis(), pendingSync = true)
        productDao.upsert(stamped.toEntity())   // 1. Se guarda al instante en Room
        ProductSyncWorker.enqueue(context)      // 2. WorkManager lo sube cuando haya internet
    }

    private fun remoteProducts(): Flow<List<Product>> = callbackFlow {
        val registration = products.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(snapshot?.documents?.mapNotNull { it.toProduct() }.orEmpty())
        }
        awaitClose { registration.remove() }
    }

    private companion object {
        const val PRODUCTS_COLLECTION = "products"
    }
}

// Público porque también lo usa ProductSyncWorker
fun Product.toFirestoreMap(): Map<String, Any?> = mapOf(
    "code" to code,
    "name" to name,
    "brand" to brand,
    "line" to line.name,
    "category" to category,
    "unit" to unit,
    "price" to price,
    "minStock" to minStock,
    "stock" to stock,
    "status" to status.name,
    "rotation" to rotation.name,
    "barcode" to barcode,
    "updatedAt" to updatedAt
)

private fun DocumentSnapshot.toProduct(): Product? {
    val name = getString("name") ?: return null
    return Product(
        code = id,
        name = name,
        brand = getString("brand").orEmpty(),
        line = ProductLine(getString("line").orEmpty()),
        category = getString("category").orEmpty(),
        unit = getString("unit").orEmpty(),
        price = getDouble("price") ?: 0.0,
        minStock = getLong("minStock")?.toInt() ?: 0,
        stock = getLong("stock")?.toInt() ?: 0,
        status = ProductStatus.fromName(getString("status").orEmpty()),
        rotation = RotationLevel.fromName(getString("rotation").orEmpty()),
        barcode = getString("barcode"),
        updatedAt = getLong("updatedAt") ?: 0L
    )
}