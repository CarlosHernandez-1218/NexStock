package com.dsmg11.nexstock.data.repository

import android.util.Log
import com.dsmg11.nexstock.domain.repository.PushRepository
import com.google.firebase.messaging.FirebaseMessaging
import javax.inject.Inject

class PushRepositoryImpl @Inject constructor() : PushRepository {

    // FCM reintenta solo si no hay internet en este momento
    override fun setAdminAlerts(enabled: Boolean) {
        val messaging = FirebaseMessaging.getInstance()
        val task = if (enabled) messaging.subscribeToTopic(ADMINS_TOPIC)
        else messaging.unsubscribeFromTopic(ADMINS_TOPIC)
        task.addOnCompleteListener {
            val action = if (enabled) "Suscrito a" else "Baja de"
            if (it.isSuccessful) Log.d(TAG, "$action tema $ADMINS_TOPIC: OK")
            else Log.e(TAG, "$action tema $ADMINS_TOPIC: ERROR", it.exception)
        }
    }

    private companion object {
        const val TAG = "NexStockPush"
        const val ADMINS_TOPIC = "admins"   // el mismo que usa la Cloud Function
    }
}
