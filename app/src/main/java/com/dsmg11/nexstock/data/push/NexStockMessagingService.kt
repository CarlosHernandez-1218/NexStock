package com.dsmg11.nexstock.data.push

import android.util.Log
import com.dsmg11.nexstock.data.notification.LogisticAlertsNotifier
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

// HU10 - MOV-70: recibe los push que envía la Cloud Function "alertasLogisticas".
// Con la app cerrada, Android muestra el push solo; con la app abierta llega aquí y lo mostramos.
class NexStockMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        Log.d("NexStockPush", "Push recibido de ${message.from}: ${message.notification?.body}")
        val title = message.notification?.title ?: message.data["title"] ?: return
        val body = message.notification?.body ?: message.data["body"].orEmpty()
        LogisticAlertsNotifier.show(applicationContext, title, body)
    }

    // Los avisos se envían por tema ("admins"), así que no hace falta guardar el token
    override fun onNewToken(token: String) = Unit
}
