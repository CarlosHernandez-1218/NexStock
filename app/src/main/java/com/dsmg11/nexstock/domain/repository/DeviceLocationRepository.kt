package com.dsmg11.nexstock.domain.repository

import com.dsmg11.nexstock.domain.model.ReceptionLocation

interface DeviceLocationRepository {

    suspend fun getCurrentLocation(): Result<ReceptionLocation>
}