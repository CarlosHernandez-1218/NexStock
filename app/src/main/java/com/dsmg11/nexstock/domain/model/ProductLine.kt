package com.dsmg11.nexstock.domain.model

enum class ProductLine(val displayName: String) {
    PISOS("Pisos"),
    SANITARIOS("Sanitarios"),
    GRIFERIA("Grifería"),
    PINTURAS("Pinturas"),
    MATERIALES_CONSTRUCCION("Materiales de construcción"),
    FERRETERIA("Ferretería");

    companion object {
        fun fromName(name: String): ProductLine? = entries.firstOrNull { it.name == name }
    }
}