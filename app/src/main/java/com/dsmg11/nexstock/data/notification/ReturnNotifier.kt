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
import com.dsmg11.nexstock.domain.model.RETURN_LIMIT_DAYS
import com.dsmg11.nexstock.domain.model.SupplierReturn
import com.dsmg11.nexstock.domain.model.WarehouseAlert
import com.dsmg11.nexstock.domain.model.daysInWarehouse
import com.dsmg11.nexstock.domain.model.warehouseAlert

// HU09: notificaciones locales de devoluciones a proveedor
object ReturnNotifier {

    private const val CHANNEL_ID = "return_alerts"
    private const val ALERT_ID = 1006      // MOV-63: plazo de 7 días (1005 queda para MOV-62)
    private const val ALERT_REQUEST_CODE = 4

    // MOV-63: devoluciones cerca del límite o que ya lo pasaron
    @SuppressLint("MissingPermission")
    fun showWarehouseAlert(context: Context, alerts: List<SupplierReturn>, now: Long) {
        if (!canNotify(context)) return
        createChannel(context)

        val over = alerts.count { it.warehouseAlert(now) == WarehouseAlert.OVER_LIMIT }
        val near = alerts.size - over
        val summary = listOfNotNull(
            if (near > 0) "$near por vencer" else null,
            if (over > 0) "$over pasaron el límite" else null
        ).joinToString(" · ")
        val detail = alerts
            .sortedByDescending { it.daysInWarehouse(now) }
            .take(5)
            .joinToString("\n") {
                "${it.productName} · ${it.quantity} ${it.unit} — ${it.daysInWarehouse(now)} días"
            }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("Devoluciones cerca del límite de $RETURN_LIMIT_DAYS días")
            .setContentText(summary)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$summary\n$detail"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openApp(context, ALERT_REQUEST_CODE))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(ALERT_ID, notification)
    }

    private fun openApp(context: Context, requestCode: Int): PendingIntent =
        PendingIntent.getActivity(
            context,
            requestCode,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE
        )

    private fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Devoluciones a proveedor",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Plazo de 7 días en almacén y recordatorio de recojo" }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}