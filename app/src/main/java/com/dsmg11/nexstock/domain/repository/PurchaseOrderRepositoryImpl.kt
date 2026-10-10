package com.dsmg11.nexstock.data.repository

import com.dsmg11.nexstock.domain.model.OrderStatus
import com.dsmg11.nexstock.domain.model.PurchaseOrder
import com.dsmg11.nexstock.domain.repository.PurchaseOrderRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class PurchaseOrderRepositoryImpl @Inject constructor(
    firestore: FirebaseFirestore
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
            collection.document(orderId).update("status", status.name).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(IllegalStateException("No se pudo actualizar el estado de la orden", e))
        }
}