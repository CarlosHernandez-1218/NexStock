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

// HU10 - MOV-70: notificaciones push de stock crítico y órdenes vencidas
object LogisticAlertsNotifier {

    const val CHANNEL_ID = "logistic_alerts"   // el mismo que usa la Cloud Function
    private const val NOTIFICATION_ID = 1003    // 1001 stock, 1002 órdenes pendientes

    // Se crea al abrir la app, para que los push con la app cerrada ya tengan su canal
    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alertas logísticas",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Resumen diario de stock crítico y órdenes vencidas (Administrador)" }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    // Se usa cuando el push llega con la app abierta (Android no lo muestra solo en ese caso)
    @SuppressLint("MissingPermission")
    fun show(context: Context, title: String, body: String) {
        val allowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        if (!allowed) return
        createChannel(context)

        val openApp = PendingIntent.getActivity(
            context,
            3,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}
