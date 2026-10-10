package com.dsmg11.nexstock.domain.model

// Cantidad sugerida al marcar un candidato: lleva el stock al doble del mínimo
// (siempre al menos 1). El usuario puede cambiarla.
val Product.suggestedQuantity: Int
    get() = (minStock * 2 - stock).coerceAtLeast(1)