package com.dsmg11.nexstock.domain.usecase.reception

import com.dsmg11.nexstock.domain.model.ReceptionLocation
import com.dsmg11.nexstock.domain.model.ReceptionLocationValidation
import com.dsmg11.nexstock.domain.model.ReceptionSite
import com.dsmg11.nexstock.domain.model.ReceptionSiteConfig
import javax.inject.Inject
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class ValidateReceptionLocationUseCase @Inject constructor() {

    operator fun invoke(
        location: ReceptionLocation,
        site: ReceptionSite =
            ReceptionSiteConfig.MAIN_WAREHOUSE
    ): ReceptionLocationValidation {

        val distance = calculateDistanceMeters(
            latitude1 = location.latitude,
            longitude1 = location.longitude,
            latitude2 = site.latitude,
            longitude2 = site.longitude
        )

        return ReceptionLocationValidation(
            site = site,
            distanceMeters = distance,
            isValid = distance <= site.allowedRadiusMeters
        )
    }

    private fun calculateDistanceMeters(
        latitude1: Double,
        longitude1: Double,
        latitude2: Double,
        longitude2: Double
    ): Double {

        val earthRadius = 6_371_000.0

        val lat1 = Math.toRadians(latitude1)
        val lat2 = Math.toRadians(latitude2)

        val deltaLat =
            Math.toRadians(latitude2 - latitude1)

        val deltaLon =
            Math.toRadians(longitude2 - longitude1)

        val a =
            sin(deltaLat / 2).pow(2) +
                    cos(lat1) *
                    cos(lat2) *
                    sin(deltaLon / 2).pow(2)

        val c =
            2 * asin(
                sqrt(a)
            )

        return earthRadius * c
    }
}