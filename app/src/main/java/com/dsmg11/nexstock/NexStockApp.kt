package com.dsmg11.nexstock

import android.app.Application
import com.dsmg11.nexstock.data.notification.LogisticAlertsNotifier
import com.dsmg11.nexstock.data.sync.ProductSyncWorker
import com.dsmg11.nexstock.data.sync.ReceptionSyncWorker
import com.dsmg11.nexstock.data.sync.StockReviewWorker
import com.dsmg11.nexstock.data.sync.PendingOrdersReminderWorker
import com.dsmg11.nexstock.data.sync.ReturnDeadlineWorker
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class NexStockApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Si quedaron cambios sin subir de una sesión anterior, se programan de nuevo
        ProductSyncWorker.enqueue(this)
        ReceptionSyncWorker.enqueue(this)
        PendingOrdersReminderWorker.schedule(this)
        StockReviewWorker.schedule(this)   // MOV-69: revisión de stock cada 6 horas
        LogisticAlertsNotifier.createChannel(this)   // MOV-70: canal de los push
        ReturnDeadlineWorker.schedule(this)   // HU09 MOV-63: control diario del plazo de 7 días
    }
}