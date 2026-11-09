package com.dsmg11.nexstock.domain.usecase.reception

import com.dsmg11.nexstock.domain.model.ReceptionLocation
import com.dsmg11.nexstock.domain.repository.DeviceLocationRepository
import javax.inject.Inject

class GetCurrentReceptionLocationUseCase @Inject constructor(
    private val locationRepository: DeviceLocationRepository
) {

    suspend operator fun invoke():
            Result<ReceptionLocation> {

        return locationRepository.getCurrentLocation()
    }
}