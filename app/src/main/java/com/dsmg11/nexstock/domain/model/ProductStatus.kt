package com.dsmg11.nexstock.domain.model

enum class ProductStatus(val displayName: String) {
    ACTIVO("Activo"),
    INACTIVO("Inactivo"),
    DESCONTINUADO("Descontinuado");

    companion object {
        fun fromName(name: String): ProductStatus =
            entries.firstOrNull { it.name == name } ?: ACTIVO
    }
}