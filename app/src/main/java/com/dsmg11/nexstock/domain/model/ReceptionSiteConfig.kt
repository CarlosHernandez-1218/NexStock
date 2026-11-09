package com.dsmg11.nexstock.domain.model

object ReceptionSiteConfig {

    /*
     * HU08 - MOV-57
     *
     * Ubicación utilizada para validar la recepción.
     *
     * Para la demostración debes colocar aquí las mismas
     * coordenadas que configures en el GPS del emulador.
     *
     * Posteriormente esta información puede venir de Firestore.
     */
    val MAIN_WAREHOUSE = ReceptionSite(
        id = "ALM-001",
        name = "Almacén Principal",
        latitude = -12.0464,
        longitude = -77.0428,
        allowedRadiusMeters = 200.0
    )
}