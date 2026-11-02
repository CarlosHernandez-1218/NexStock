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

object PendingOrdersNotifier {

    private const val CHANNEL_ID = "pending_orders_reminder"
    private const val NOTIFICATION_ID = 1002   // la alerta de stock usa 1001

    @SuppressLint("MissingPermission")
    fun show(context: Context, total: Int, overdue: Int, dueSoon: Int): Boolean {
        if (!canNotify(context)) return false
        createChannel(context)

        val text = listOfNotNull(
            "$total pendientes",
            if (overdue > 0) "$overdue vencidas" else null,
            if (dueSoon > 0) "$dueSoon por vencer" else null
        ).joinToString(" · ")

        val openApp = PendingIntent.getActivity(
            context,
            1,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Revisa tus órdenes de compra pendientes")
            .setContentText(text)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        return true
    }

    private fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Recordatorio de órdenes pendientes",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Aviso dos veces por semana para revisar el reporte de órdenes" }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}