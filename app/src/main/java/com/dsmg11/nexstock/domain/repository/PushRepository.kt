package com.dsmg11.nexstock.domain.repository

interface PushRepository {
    // MOV-70: los Administradores reciben el push diario por el tema "admins"
    fun setAdminAlerts(enabled: Boolean)
}
