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
import com.dsmg11.nexstock.domain.model.Product
import com.dsmg11.nexstock.domain.model.StockLevel
import com.dsmg11.nexstock.domain.model.stockLevel
import com.dsmg11.nexstock.domain.repository.StockAlertRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StockAlertRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : StockAlertRepository {

    // Guarda qué productos ya se avisaron, para no repetir la notificación
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun alertedCodes(): Set<String> =
        prefs.getStringSet(KEY_CODES, emptySet()).orEmpty().toSet()

    override fun saveAlertedCodes(codes: Set<String>) {
        prefs.edit().putStringSet(KEY_CODES, codes).apply()
    }

    @SuppressLint("MissingPermission")
    override fun showAlert(products: List<Product>): Boolean {
        if (products.isEmpty() || !canNotify()) return false
        createChannel()

        val first = products.first()
        val title = if (products.size == 1) {
            if (first.stockLevel == StockLevel.AGOTADO) "Agotado: ${first.name}"
            else "Stock bajo: ${first.name}"
        } else {
            "${products.size} productos requieren reposición"
        }
        val text = if (products.size == 1) {
            "Stock: ${first.stock} ${first.unit} (mínimo ${first.minStock})"
        } else {
            products.joinToString { it.name }
        }
        val style = if (products.size == 1) {
            NotificationCompat.BigTextStyle().bigText(text)
        } else {
            NotificationCompat.InboxStyle().also { inbox ->
                products.take(5).forEach { p ->
                    inbox.addLine("${p.name}: ${p.stock} (mín. ${p.minStock})")
                }
            }
        }

        // Al tocar la notificación se abre la app
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(style)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        return true
    }

    // Android 13+ necesita que el usuario haya aceptado el permiso de notificaciones
    private fun canNotify(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alertas de stock",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Avisos cuando un producto llega al stock mínimo" }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private companion object {
        const val PREFS_NAME = "stock_alerts"
        const val KEY_CODES = "alerted_codes"
        const val CHANNEL_ID = "stock_alerts"
        const val NOTIFICATION_ID = 1001
    }
}