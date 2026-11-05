package com.dsmg11.nexstock.data.repository

import com.dsmg11.nexstock.domain.repository.DashboardRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class DashboardRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : DashboardRepository {

    override fun observeReturnsToPickup(): Flow<Int> = callbackFlow {
        val registration = firestore.collection(RETURNS)
            .whereEqualTo("status", PENDING_PICKUP)
            .addSnapshotListener { snapshot, error ->
                // Si la colección aún no existe o las reglas no la permiten, el KPI muestra 0
                trySend(if (error != null) 0 else snapshot?.size() ?: 0)
            }
        awaitClose { registration.remove() }
    }

    override fun observeReceptionTimesSince(fromMillis: Long): Flow<List<Long>> = callbackFlow {
        val registration = firestore.collection(RECEPTIONS)
            .whereGreaterThanOrEqualTo("receivedAt", fromMillis)
            .addSnapshotListener { snapshot, error ->
                val times = if (error != null) emptyList()
                else snapshot?.documents?.mapNotNull { it.getLong("receivedAt") }.orEmpty()
                trySend(times)
            }
        awaitClose { registration.remove() }
    }

    private companion object {
        // Acordado con Carlos para la HU09
        const val RETURNS = "devoluciones"
        const val PENDING_PICKUP = "PENDIENTE_RECOJO"
        // HU07
        const val RECEPTIONS = "recepciones"
    }
}
