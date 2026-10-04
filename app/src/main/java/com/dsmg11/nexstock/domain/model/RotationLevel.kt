package com.dsmg11.nexstock.domain.model

enum class RotationLevel(val level: Int, val shortName: String) {
    ALTA(1, "Alta"),
    MEDIA(2, "Mediana"),
    BAJA(3, "Baja");

    val displayName: String get() = "Nivel $level - $shortName"

    companion object {
        fun fromName(name: String): RotationLevel =
            entries.firstOrNull { it.name == name } ?: MEDIA
    }
}