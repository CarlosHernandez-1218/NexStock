package com.dsmg11.nexstock.domain.model

data class MasterItem(
    val id: String,
    val name: String,
    val active: Boolean = true,
    val parentId: String? = null   // Solo categorías: código de la línea (ej. "PISOS")
)