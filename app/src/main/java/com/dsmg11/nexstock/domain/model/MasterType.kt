package com.dsmg11.nexstock.domain.model

// Tipos de datos maestros y la colección de Firestore donde vive cada uno
enum class MasterType(val collection: String, val title: String) {
    LINEAS("product_lines", "Líneas"),
    CATEGORIAS("categories", "Categorías"),
    UNIDADES("units", "Unidades")
}