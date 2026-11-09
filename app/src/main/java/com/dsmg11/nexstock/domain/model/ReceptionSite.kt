package com.dsmg11.nexstock.domain.model

data class ReceptionSite(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val allowedRadiusMeters: Double
)

data class ReceptionLocationValidation(
    val site: ReceptionSite,
    val distanceMeters: Double,
    val isValid: Boolean
)