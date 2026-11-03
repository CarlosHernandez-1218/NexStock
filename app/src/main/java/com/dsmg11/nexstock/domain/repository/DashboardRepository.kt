package com.dsmg11.nexstock.domain.repository

import kotlinx.coroutines.flow.Flow

interface DashboardRepository {
    // HU09 (Carlos): devoluciones en estado Pendiente de recojo. Es 0 mientras la colección no exista.
    fun observeReturnsToPickup(): Flow<Int>
}
