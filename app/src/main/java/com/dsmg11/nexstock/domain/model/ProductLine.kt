package com.dsmg11.nexstock.domain.model

// Línea de productos. Se lee de la colección product_lines.
// "name" es el código (ej. PISOS) que se guarda en productos, usuarios y reglas de Firestore.
data class ProductLine(
    val name: String,
    val displayName: String = name
) {
    override fun toString(): String = displayName
}

// Compara solo por código: PISOS es la misma línea aunque su nombre aún no haya cargado
fun ProductLine.sameAs(other: ProductLine?): Boolean = other != null && name == other.name

// Completa el nombre visible con el mapa código → nombre de product_lines
fun ProductLine.withName(names: Map<String, String>): ProductLine =
    ProductLine(name, names[name] ?: displayName)