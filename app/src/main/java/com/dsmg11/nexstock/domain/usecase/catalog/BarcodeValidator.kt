package com.dsmg11.nexstock.domain.usecase.catalog

// MOV-21: códigos de barras de productos (estándar GS1): EAN-8, UPC-A (12) y EAN-13.
// En Perú los productos llevan EAN-13 y empiezan con 775.
object BarcodeValidator {

    private val VALID_LENGTHS = setOf(8, 12, 13)

    // Devuelve el motivo del error, o null si el código es válido
    fun error(barcode: String): String? = when {
        barcode.any { !it.isDigit() } -> "El código de barras solo puede tener números"
        barcode.length !in VALID_LENGTHS -> "Debe tener 8, 12 o 13 dígitos (EAN-8, UPC-A o EAN-13)"
        !hasValidCheckDigit(barcode) -> "Código inválido: el dígito verificador no coincide"
        else -> null
    }

    // Algoritmo GS1: de derecha a izquierda (sin el último dígito) se multiplica por 3, 1, 3, 1…
    // El último dígito debe ser lo que falta para llegar a la siguiente decena
    private fun hasValidCheckDigit(barcode: String): Boolean {
        val digits = barcode.map { it - '0' }
        val sum = digits.dropLast(1).reversed()
            .mapIndexed { i, d -> if (i % 2 == 0) d * 3 else d }
            .sum()
        return (10 - sum % 10) % 10 == digits.last()
    }
}