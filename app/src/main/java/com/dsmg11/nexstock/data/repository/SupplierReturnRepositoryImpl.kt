package com.dsmg11.nexstock.data.repository

import com.dsmg11.nexstock.domain.model.SupplierReturn
import com.dsmg11.nexstock.domain.repository.SupplierReturnRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

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
}