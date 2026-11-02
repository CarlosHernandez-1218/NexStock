package com.dsmg11.nexstock.data.notification

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.dsmg11.nexstock.MainActivity
import com.dsmg11.nexstock.domain.model.OrderExtension
import com.dsmg11.nexstock.domain.repository.ExtensionAlertRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExtensionAlertRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ExtensionAlertRepository {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun lastSeen(): Long = prefs.getLong(KEY_LAST_SEEN, 0L)

    override fun saveLastSeen(timestamp: Long) {
        prefs.edit().putLong(KEY_LAST_SEEN, timestamp).apply()
    }

    @SuppressLint("MissingPermission")
    override fun showAlert(extensions: List<OrderExtension>): Boolean {
        if (extensions.isEmpty() || !canNotify()) return false
        createChannel()

        val title = if (extensions.size == 1) "Prórroga de entrega registrada"
        else "${extensions.size} prórrogas de entrega registradas"
        val lines = extensions.map { e ->
            "OC-${e.orderId.takeLast(6).uppercase()} · ${e.supplierName}: " +
                    "${formatDate(e.previousDate)} → ${formatDate(e.newDate)} (${e.reason})"
        }

        val openApp = PendingIntent.getActivity(
            context,
            2,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(lines.first())
            .setStyle(NotificationCompat.BigTextStyle().bigText(lines.joinToString("\n")))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        return true
    }

    // Las fechas de entrega se guardan en UTC: se formatean en UTC para que no cambie el día
    private fun formatDate(utcMillis: Long): String =
        SimpleDateFormat("dd/MM/yyyy", Locale("es", "PE")).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date(utcMillis))

    private fun canNotify(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Prórrogas de entrega",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Avisos cuando un Jefe de Línea cambia la fecha de entrega de una orden" }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private companion object {
        const val PREFS_NAME = "extension_alerts"
        const val KEY_LAST_SEEN = "last_seen"
        const val CHANNEL_ID = "extension_alerts"
        const val NOTIFICATION_ID = 1003   // 1001 stock, 1002 recordatorio
    }
}