package com.dsmg11.nexstock.data.sync

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.dsmg11.nexstock.data.local.ReceptionDao
import com.dsmg11.nexstock.data.local.toDomain
import com.dsmg11.nexstock.data.repository.toFirestoreMap
import com.dsmg11.nexstock.domain.model.OrderStatus
import com.dsmg11.nexstock.domain.model.PurchaseOrder
import com.dsmg11.nexstock.domain.model.Reception
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Transaction
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import java.util.concurrent.TimeUnit
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage

// MOV-51: sube a Firestore las recepciones guardadas sin conexión
class ReceptionSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Dependencies {
        fun receptionDao(): ReceptionDao
        fun firestore(): FirebaseFirestore
        fun storage(): FirebaseStorage
    }

    override suspend fun doWork(): Result {
        val deps = EntryPointAccessors.fromApplication(applicationContext, Dependencies::class.java)
        val dao = deps.receptionDao()
        val firestore = deps.firestore()
        val storage = deps.storage()

        for (entity in dao.getPending()) {
            val reception = entity.toDomain()
            try {
                val rejection = withTimeout(30_000) {
                    // HU08: primero se suben las evidencias fotográficas.
                    val receptionWithPhotos = uploadIncidentPhotos(storage, reception)
                    upload(firestore, receptionWithPhotos) }
                if (rejection != null) Log.w(TAG, "Recepción ${reception.id} no se aplicó: $rejection")
                dao.markSynced(reception.id)
            } catch (e: FirebaseFirestoreException) {
                if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    // Las reglas la rechazaron: reintentar no cambiaría nada
                    Log.w(TAG, "Recepción ${reception.id} rechazada por las reglas", e)
                    dao.markSynced(reception.id)
                } else {
                    return Result.retry()
                }
            } catch (e: Exception) {
                return Result.retry()   // Sin red o tardó demasiado: WorkManager reintenta
            }
        }
        return Result.success()
    }
    private suspend fun uploadIncidentPhotos(
        storage: FirebaseStorage,
        reception: Reception
    ): Reception {

        val updatedIncidents = reception.incidents.map { incident ->

            val localUri = incident.photoLocalUri

            // Si no tiene foto o ya tiene URL remota, no hacemos nada.
            if (
                localUri.isNullOrBlank() ||
                !incident.photoUrl.isNullOrBlank()
            ) {
                return@map incident
            }

            // Ruta determinística:
            // si WorkManager reintenta, reutiliza el mismo archivo.
            val fileName =
                "${incident.productCode}_${incident.type.name}.jpg"

            val photoRef = storage.reference.child(
                "reception_incidents/${reception.id}/$fileName"
            )

            photoRef
                .putFile(Uri.parse(localUri))
                .await()

            val downloadUrl =
                photoRef.downloadUrl.await().toString()

            incident.copy(
                photoUrl = downloadUrl
            )
        }

        return reception.copy(
            incidents = updatedIncidents
        )
    }
    // Una sola transacción: crea la recepción, suma el stock y actualiza la orden.
    // Si la recepción ya existe (se subió antes), no hace nada: así el stock nunca se suma dos veces.
    private suspend fun upload(firestore: FirebaseFirestore, reception: Reception): String? {
        val receptionRef = firestore.collection(RECEPTIONS).document(reception.id)
        val orderRef = firestore.collection(ORDERS).document(reception.orderId)
        val products = firestore.collection(PRODUCTS)

        return firestore.runTransaction(Transaction.Function<String?> { tx ->
            if (tx.get(receptionRef).exists()) return@Function null

            val orderSnap = tx.get(orderRef)
            val order = orderSnap.toObject(PurchaseOrder::class.java)
                ?: return@Function "La orden ya no existe"
            if (order.status != OrderStatus.ENVIADA && order.status != OrderStatus.PARCIALMENTE_RECIBIDA) {
                return@Function "La orden está en ${order.status.displayName}"
            }

            val newReceived = order.received.toMutableMap()
            reception.items.forEach { newReceived[it.code] = (newReceived[it.code] ?: 0) + it.received }
            val complete = order.items.all { (newReceived[it.code] ?: 0) >= it.quantity }
            val newStatus = if (complete) OrderStatus.RECIBIDA else OrderStatus.PARCIALMENTE_RECIBIDA

            tx.set(receptionRef, reception.toFirestoreMap())
            tx.update(orderRef, mapOf("received" to newReceived, "status" to newStatus.name))
            reception.items.forEach { item ->
                tx.update(products.document(item.code), "stock", FieldValue.increment(item.received.toLong()))
            }
            null
        }).await()
    }

    companion object {
        private const val TAG = "ReceptionSync"
        private const val WORK_NAME = "reception_sync"
        private const val RECEPTIONS = "recepciones"
        private const val ORDERS = "ordenes_compra"
        private const val PRODUCTS = "products"

        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<ReceptionSyncWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)   // Solo con internet
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
        }
    }
}
