package com.dsmg11.nexstock

import android.app.Application
import com.dsmg11.nexstock.data.sync.ProductSyncWorker
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class NexStockApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Si quedaron cambios sin subir de una sesión anterior, se programan de nuevo
        ProductSyncWorker.enqueue(this)
    }
}