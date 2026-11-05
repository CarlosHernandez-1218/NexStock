package com.dsmg11.nexstock.data.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.dsmg11.nexstock.data.local.ProductDao
import com.dsmg11.nexstock.data.local.UserProfileDao
import com.dsmg11.nexstock.data.local.toDomain
import com.dsmg11.nexstock.domain.repository.ProductRepository
import com.dsmg11.nexstock.domain.usecase.catalog.CatalogPermissions
import com.dsmg11.nexstock.domain.usecase.catalog.CheckStockAlertsUseCase
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeUnit

// HU10 - MOV-69: revisión periódica del stock, aunque la app esté cerrada.
// Usa la misma regla que la alerta de MOV-31 (avisa una vez por producto que llega al mínimo).
class StockReviewWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Dependencies {
        fun auth(): FirebaseAuth
        fun userProfileDao(): UserProfileDao
        fun productDao(): ProductDao
        fun productRepository(): ProductRepository
        fun checkStockAlerts(): CheckStockAlertsUseCase
    }

    override suspend fun doWork(): Result {
        val deps = EntryPointAccessors.fromApplication(applicationContext, Dependencies::class.java)

        // Sin sesión no hay a quién avisar
        val uid = deps.auth().currentUser?.uid ?: return Result.success()
        val profile = deps.userProfileDao().observe(uid).first()?.toDomain() ?: return Result.success()

        // 1. Con internet, trae el stock actual de Firestore; sin internet, revisa la copia de Room
        withTimeoutOrNull(20_000) { deps.productRepository().refreshFromRemote() }

        // 2. Revisa los productos que este usuario puede ver (el Jefe de Línea, solo sus líneas)
        val products = deps.productDao().observeAll().first().map { it.toDomain() }
        val visible = CatalogPermissions.visibleProducts(profile, products)
        deps.checkStockAlerts()(visible)
        Log.d(TAG, "Revisión de stock: ${visible.size} productos revisados para ${profile.role}")

        return Result.success()
    }

    companion object {
        private const val TAG = "StockReview"
        private const val WORK_NAME = "stock_review"
        private const val EVERY_HOURS = 6L

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<StockReviewWorker>(EVERY_HOURS, TimeUnit.HOURS).build()
            // KEEP: si ya está programada no se reinicia cada vez que se abre la app
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
