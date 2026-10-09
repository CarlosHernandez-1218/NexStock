package com.dsmg11.nexstock.data.repository

import com.dsmg11.nexstock.domain.model.MasterItem
import com.dsmg11.nexstock.domain.model.MasterType
import com.dsmg11.nexstock.domain.repository.MasterDataRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class MasterDataRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : MasterDataRepository {

    // Firestore guarda caché local: sin conexión se siguen viendo los últimos datos
    override fun observe(type: MasterType): Flow<List<MasterItem>> = callbackFlow {
        val registration = firestore.collection(type.collection)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull { doc ->
                    val name = doc.getString("name") ?: return@mapNotNull null
                    MasterItem(
                        id = doc.id,
                        name = name,
                        active = doc.getBoolean("active") ?: true,
                        parentId = doc.getString("lineId")
                    )
                }.orEmpty().sortedBy { it.name.lowercase() }
                trySend(items)
            }
        awaitClose { registration.remove() }
    }

    override suspend fun add(
        type: MasterType,
        id: String?,
        name: String,
        parentId: String?
    ): Result<Unit> = runCatching {
        val data = buildMap<String, Any> {
            put("name", name)
            put("active", true)
            if (parentId != null) put("lineId", parentId)
        }
        val collection = firestore.collection(type.collection)
        val document = if (id != null) collection.document(id) else collection.document()
        document.set(data)   // Sin await: sin conexión queda en cola y se ve al instante
    }

    // No se borra nada: se desactiva para no romper productos que ya lo usan
    override suspend fun setActive(type: MasterType, id: String, active: Boolean): Result<Unit> =
        runCatching {
            firestore.collection(type.collection).document(id).update("active", active)
        }
}