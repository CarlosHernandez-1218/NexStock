package com.dsmg11.nexstock.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.dsmg11.nexstock.data.local.ProductDao
import com.dsmg11.nexstock.data.local.toDomain
import com.dsmg11.nexstock.data.repository.toFirestoreMap
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import java.util.concurrent.TimeUnit

class ProductSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    // Hilt entrega al Worker el DAO y Firestore
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Dependencies {
        fun productDao(): ProductDao
        fun firestore(): FirebaseFirestore
    }

    override suspend fun doWork(): Result {
        val deps = EntryPointAccessors.fromApplication(applicationContext, Dependencies::class.java)
        val dao = deps.productDao()
        val products = deps.firestore().collection("products")

        for (entity in dao.getPending()) {
            try {
                withTimeout(30_000) {
                    products.document(entity.code).set(entity.toDomain().toFirestoreMap()).await()
                }
                dao.markSynced(entity.code, entity.updatedAt)
            } catch (e: FirebaseFirestoreException) {
                if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    // Las reglas rechazaron el cambio: no tiene sentido reintentarlo
                    dao.markSynced(entity.code, entity.updatedAt)
                } else {
                    return Result.retry()
                }
            } catch (e: Exception) {
                return Result.retry()   // Se cortó la red o tardó demasiado: WorkManager reintenta
            }
        }
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "product_sync"

        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<ProductSyncWorker>()
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