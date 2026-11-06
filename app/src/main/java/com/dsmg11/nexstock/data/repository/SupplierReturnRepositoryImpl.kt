package com.dsmg11.nexstock.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Transaction
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import com.dsmg11.nexstock.domain.model.SupplierReturn
import com.dsmg11.nexstock.domain.repository.SupplierReturnRepository
import com.dsmg11.nexstock.domain.model.ReturnStatus

class SupplierReturnRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : SupplierReturnRepository {

    private val collection get() = firestore.collection("devoluciones")

    override fun observeReturns(): Flow<List<SupplierReturn>> = callbackFlow {
        val registration = collection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents.orEmpty().mapNotNull { doc ->
                    doc.toObject(SupplierReturn::class.java)?.copy(id = doc.id)
                })
            }
        awaitClose { registration.remove() }
    }

    override suspend fun register(supplierReturn: SupplierReturn): Result<Unit> = try {
        val ref = collection.document()
        ref.set(supplierReturn.copy(id = ref.id)).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(IllegalStateException("No se pudo registrar la devolución", e))
    }

    override suspend fun moveTo(
        returnId: String,
        target: ReturnStatus,
        fields: Map<String, Any>
    ): Result<Unit> = try {
        val ref = collection.document(returnId)
        val rejection: String? = firestore.runTransaction(Transaction.Function<String?> { tx ->
            val snapshot = tx.get(ref)
            val current = ReturnStatus.entries.firstOrNull { it.name == snapshot.getString("status") }
                ?: return@Function "La devolución no existe"
            if (!current.canMoveTo(target)) {
                return@Function "La devolución está en ${current.displayName}; no puede pasar a ${target.displayName}"
            }
            tx.update(ref, fields + ("status" to target.name))
            null
        }).await()

        if (rejection != null) Result.failure(IllegalStateException(rejection))
        else Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(IllegalStateException("No se pudo actualizar la devolución", e))
    }
}