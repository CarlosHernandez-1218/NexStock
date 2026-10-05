package com.dsmg11.nexstock.domain.model

enum class StockLevel(val displayName: String) {
    NORMAL("Normal"),
    BAJO("Bajo"),
    AGOTADO("Agotado")
}

// Agotado: sin existencias. Bajo: llegó al stock mínimo o menos. Normal: por encima del mínimo.
val Product.stockLevel: StockLevel
    get() = when {
        stock <= 0 -> StockLevel.AGOTADO
        stock <= minStock -> StockLevel.BAJO
        else -> StockLevel.NORMAL
    }