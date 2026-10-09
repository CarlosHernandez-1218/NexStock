package com.dsmg11.nexstock.domain.model

// Línea de productos. Ya no es una lista fija: se lee de la colección product_lines.
// "name" es el código (ej. PISOS) que se guarda en productos, usuarios y reglas de Firestore.
class ProductLine(
    val name: String,
    val displayName: String = name
) {
    // Dos líneas son la misma si tienen el mismo código
    override fun equals(other: Any?): Boolean = other is ProductLine && other.name == name
    override fun hashCode(): Int = name.hashCode()
    override fun toString(): String = displayName
}

// Completa el nombre visible con el mapa código → nombre de product_lines
fun ProductLine.withName(names: Map<String, String>): ProductLine =
    ProductLine(name, names[name] ?: displayName)