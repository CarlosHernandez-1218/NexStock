package com.dsmg11.nexstock.data.repository

import com.dsmg11.nexstock.domain.model.OrderStatus
import com.dsmg11.nexstock.domain.model.PurchaseOrder
import com.dsmg11.nexstock.domain.repository.PurchaseOrderRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Transaction
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class PurchaseOrderRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : PurchaseOrderRepository {

    private val collection = firestore.collection("ordenes_compra")

    override suspend fun createOrder(order: PurchaseOrder): Result<String> =
        try {
            val document = collection.document()          // genera el id
            document.set(order.copy(id = document.id)).await()
            Result.success(document.id)
        } catch (e: Exception) {
            Result.failure(IllegalStateException("No se pudo guardar la orden de compra", e))
        }

    override suspend fun updateStatus(orderId: String, status: OrderStatus): Result<Unit> =
        try {
            val ref = collection.document(orderId)
            // La transacción lee el estado actual y solo escribe si la transición sigue siendo válida
            val rejection: String? = firestore.runTransaction(Transaction.Function<String?> { tx ->
                val snapshot = tx.get(ref)
                val current = OrderStatus.entries.firstOrNull { it.name == snapshot.getString("status") }
                    ?: return@Function "La orden no existe o no tiene un estado válido"
                if (!current.canChangeTo(status)) {
                    return@Function "La orden ya está en ${current.displayName}; no se puede pasar a ${status.displayName}"
                }
                tx.update(ref, "status", status.name)
                null
            }).await()

            if (rejection != null) Result.failure(IllegalStateException(rejection))
            else Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(IllegalStateException("No se pudo actualizar el estado de la orden", e))
        }

    override fun observeOrders(): Flow<List<PurchaseOrder>> = callbackFlow {
        val registration = collection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val orders = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    doc.toObject(PurchaseOrder::class.java)?.copy(id = doc.id)
                }
                trySend(orders)
            }
        awaitClose { registration.remove() }
    }
}