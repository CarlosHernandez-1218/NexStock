package com.dsmg11.nexstock.domain.model

// Conjunto vacío = ese filtro no restringe nada
data class StockFilters(
    val brands: Set<String> = emptySet(),
    val categories: Set<String> = emptySet(),
    val rotations: Set<RotationLevel> = emptySet(),
    val levels: Set<StockLevel> = emptySet()
) {
    val isEmpty: Boolean
        get() = brands.isEmpty() && categories.isEmpty() && rotations.isEmpty() && levels.isEmpty()
}