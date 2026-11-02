package com.dsmg11.nexstock.domain.repository

import com.dsmg11.nexstock.domain.model.OrderExtension

interface ExtensionAlertRepository {
    // Marca de tiempo de la última prórroga avisada en este celular (0 = nunca)
    fun lastSeen(): Long
    fun saveLastSeen(timestamp: Long)
    // Devuelve true si la notificación se mostró (false si no hay permiso)
    fun showAlert(extensions: List<OrderExtension>): Boolean
}