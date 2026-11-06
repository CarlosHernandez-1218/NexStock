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
import com.dsmg11.nexstock.domain.model.UserRole
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

// HU09 - MOV-62: los viernes recuerda revisar los almacenes y coordinar el recojo con el proveedor
class ReturnPickupReminderWorker(
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

        val uid = deps.auth().currentUser?.uid ?: return Result.success()
        val profile = deps.userProfileDao().observe(uid).first()?.toDomain() ?: return Result.success()

        // El recordatorio es para el Jefe de Línea, que es quien coordina el recojo
        if (profile.role != UserRole.JEFE_LINEA) return Result.success()

        return try {
            // Lo que aún está en almacén: todo menos Recogida
            val inWarehouse = ReturnStatus.entries.filter { it != ReturnStatus.RECOGIDA }.map { it.name }
            val returns = withTimeout(30_000) {
                deps.firestore().collection("devoluciones").whereIn("status", inWarehouse).get().await()
            }.documents.mapNotNull { doc ->
                doc.toObject(SupplierReturn::class.java)?.copy(id = doc.id)
            }

            val mine = ReturnPermissions.visibleReturns(profile, returns)
            if (mine.isNotEmpty()) {
                ReturnNotifier.showPickupReminder(
                    context = applicationContext,
                    toAuthorize = mine.count { it.status == ReturnStatus.REGISTRADA },
                    toCoordinate = mine.count { it.status == ReturnStatus.AUTORIZADA },
                    waiting = mine.count { it.status == ReturnStatus.PENDIENTE_RECOJO }
                )
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()   // sin red o Firestore tardó demasiado
        }
    }

    companion object {
        private const val WORK_NAME = "return_pickup_reminder_friday"
        private const val REMINDER_HOUR = 9

        // Una vez por semana: los viernes cerca de las 9:00 (hora local)
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ReturnPickupReminderWorker>(7, TimeUnit.DAYS)
                .setInitialDelay(delayUntilFriday(), TimeUnit.MILLISECONDS)
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                )
                .build()
            // KEEP: si ya está programada no se reinicia cada vez que se abre la app
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        private fun delayUntilFriday(): Long {
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY)
                set(Calendar.HOUR_OF_DAY, REMINDER_HOUR)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= now.timeInMillis) add(Calendar.WEEK_OF_YEAR, 1)
            }
            return target.timeInMillis - now.timeInMillis
        }
    }
}