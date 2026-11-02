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
import com.dsmg11.nexstock.data.notification.PendingOrdersNotifier
import com.dsmg11.nexstock.domain.model.DeadlineStatus
import com.dsmg11.nexstock.domain.model.PurchaseOrder
import com.dsmg11.nexstock.domain.model.UserRole
import com.dsmg11.nexstock.domain.model.deadlineStatus
import com.dsmg11.nexstock.domain.usecase.replenishment.PendingOrdersReport
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

class PendingOrdersReminderWorker(
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

        // Sin sesión iniciada no hay a quién recordarle
        val uid = deps.auth().currentUser?.uid ?: return Result.success()
        val profile = deps.userProfileDao().observe(uid).first()?.toDomain()
            ?: return Result.success()

        // El recordatorio es solo para el Jefe de Línea
        if (profile.role != UserRole.JEFE_LINEA) return Result.success()

        return try {
            val orders = withTimeout(30_000) {
                deps.firestore().collection("ordenes_compra").get().await()
            }.documents.mapNotNull { doc ->
                doc.toObject(PurchaseOrder::class.java)?.copy(id = doc.id)
            }

            val pending = PendingOrdersReport.build(profile, orders)
            if (pending.isNotEmpty()) {
                PendingOrdersNotifier.show(
                    context = applicationContext,
                    total = pending.size,
                    overdue = pending.count { it.deadlineStatus() == DeadlineStatus.VENCIDA },
                    dueSoon = pending.count { it.deadlineStatus() == DeadlineStatus.POR_VENCER }
                )
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()   // sin red o Firestore tardó demasiado
        }
    }

    companion object {
        private const val WORK_MONDAY = "pending_reminder_monday"
        private const val WORK_THURSDAY = "pending_reminder_thursday"
        private const val REMINDER_HOUR = 9

        // Dos tareas semanales = dos recordatorios por semana
        fun schedule(context: Context) {
            val workManager = WorkManager.getInstance(context)
            enqueueWeekly(workManager, WORK_MONDAY, Calendar.MONDAY)
            enqueueWeekly(workManager, WORK_THURSDAY, Calendar.THURSDAY)
        }

        private fun enqueueWeekly(workManager: WorkManager, name: String, dayOfWeek: Int) {
            val request = PeriodicWorkRequestBuilder<PendingOrdersReminderWorker>(7, TimeUnit.DAYS)
                .setInitialDelay(delayUntil(dayOfWeek), TimeUnit.MILLISECONDS)
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                )
                .build()
            // KEEP: si ya está programada no se reinicia cada vez que se abre la app
            workManager.enqueueUniquePeriodicWork(name, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        // Milisegundos hasta el próximo día indicado a las 9:00 (hora local)
        private fun delayUntil(dayOfWeek: Int): Long {
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, dayOfWeek)
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