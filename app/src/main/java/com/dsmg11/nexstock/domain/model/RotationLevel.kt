package com.dsmg11.nexstock.domain.model

enum class RotationLevel(val displayName: String) {
    ALTA("Alta rotación"),
    MEDIA("Rotación media"),
    BAJA("Baja rotación");

    companion object {
        fun fromName(name: String): RotationLevel =
            entries.firstOrNull { it.name == name } ?: MEDIA
    }
}