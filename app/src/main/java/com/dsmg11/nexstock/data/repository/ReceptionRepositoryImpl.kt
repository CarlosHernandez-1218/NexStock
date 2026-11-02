package com.dsmg11.nexstock.data.repository

import android.content.Context
import com.dsmg11.nexstock.data.local.ReceptionDao
import com.dsmg11.nexstock.data.local.toDomain
import com.dsmg11.nexstock.data.local.toEntity
import com.dsmg11.nexstock.data.sync.ReceptionSyncWorker
import com.dsmg11.nexstock.domain.model.Reception
import com.dsmg11.nexstock.domain.repository.ReceptionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ReceptionRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val receptionDao: ReceptionDao
) : ReceptionRepository {

    override suspend fun register(reception: Reception): Result<Unit> = runCatching {
        receptionDao.insert(reception.copy(pendingSync = true).toEntity())  // 1. Se guarda al instante en Room
        ReceptionSyncWorker.enqueue(context)                                // 2. WorkManager la sube con internet
    }

    override fun observePending(): Flow<List<Reception>> =
        receptionDao.observePending().map { list -> list.map { it.toDomain() } }
}

// Documento de recepciones/{id} en Firestore (lo usa ReceptionSyncWorker)
fun Reception.toFirestoreMap(): Map<String, Any?> = mapOf(
    "orderId" to orderId,
    "supplierRuc" to supplierRuc,
    "supplierName" to supplierName,
    "items" to items.map {
        mapOf(
            "code" to it.code,
            "name" to it.name,
            "unit" to it.unit,
            "line" to it.line,
            "ordered" to it.ordered,
            "received" to it.received
        )
    },
    "receivedBy" to receivedBy,
    "receivedAt" to receivedAt,
    "location" to location?.let { mapOf("latitude" to it.latitude, "longitude" to it.longitude) }
)
