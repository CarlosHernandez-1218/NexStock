package com.dsmg11.nexstock.domain.model

enum class ReceptionIncidentType(
    val displayName: String
) {
    FALTANTE("Faltante"),
    SOBRANTE("Sobrante"),
    DETERIORADO("Deteriorado")
}

data class ReceptionIncident(
    val productCode: String = "",
    val productName: String = "",
    val type: ReceptionIncidentType = ReceptionIncidentType.FALTANTE,
    val observation: String = "",

    // MOV-55: se utilizarán posteriormente para la evidencia fotográfica
    val photoLocalUri: String? = null,
    val photoUrl: String? = null
)