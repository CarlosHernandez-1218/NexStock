package com.dsmg11.nexstock.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.dsmg11.nexstock.data.local.UserProfileDao
import com.dsmg11.nexstock.data.local.toDomain
import com.dsmg11.nexstock.data.notification.ReturnNotifier
import com.dsmg11.nexstock.domain.model.ReturnStatus
import com.dsmg11.nexstock.domain.model.SupplierReturn
import com.dsmg11.nexstock.domain.model.warehouseAlert
import com.dsmg11.nexstock.domain.usecase.returns.ReturnPermissions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import java.util.Calendar
import java.util.concurrent.TimeUnit

// HU09 - MOV-63: control diario del plazo de 7 días de las devoluciones en almacén
class ReturnDeadlineWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Dependencies {
        fun auth(): FirebaseAuth
        fun firestore(): FirebaseFirestore
        fun userProfileDao(): UserProfileDao
    }

    override suspend fun doWork(): Result {
        val deps = EntryPointAccessors.fromApplication(applicationContext, Dependencies::class.java)

        // Sin sesión no hay a quién avisar
        val uid = deps.auth().currentUser?.uid ?: return Result.success()
        val profile = deps.userProfileDao().observe(uid).first()?.toDomain() ?: return Result.success()

        return try {
            // Solo las que aún están en almacén (todas menos Recogida)
            val inWarehouse = ReturnStatus.entries.filter { it != ReturnStatus.RECOGIDA }.map { it.name }
            val returns = withTimeout(30_000) {
                deps.firestore().collection("devoluciones").whereIn("status", inWarehouse).get().await()
            }.documents.mapNotNull { doc ->
                doc.toObject(SupplierReturn::class.java)?.copy(id = doc.id)
            }

            // Mismo filtro que la lista: Jefe de Línea sus líneas, Administrador todas, Almacenero ninguna
            val now = System.currentTimeMillis()
            val alerts = ReturnPermissions.visibleReturns(profile, returns)
                .filter { it.warehouseAlert(now) != null }
            if (alerts.isNotEmpty()) ReturnNotifier.showWarehouseAlert(applicationContext, alerts, now)
            Result.success()
        } catch (e: Exception) {
            Result.retry()   // sin red o Firestore tardó demasiado
        }
    }

    companion object {
        private const val WORK_NAME = "return_deadline_daily"
        private const val CHECK_HOUR = 8

        // Una vez al día, cerca de las 8:00 (hora local)
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ReturnDeadlineWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delayUntilNext(CHECK_HOUR), TimeUnit.MILLISECONDS)
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                )
                .build()
            // KEEP: si ya está programada no se reinicia cada vez que se abre la app
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        private fun delayUntilNext(hour: Int): Long {
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= now.timeInMillis) add(Calendar.DAY_OF_YEAR, 1)
            }
            return target.timeInMillis - now.timeInMillis
        }
    }
}