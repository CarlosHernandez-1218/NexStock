package com.dsmg11.nexstock.domain.usecase.catalog

import com.dsmg11.nexstock.domain.model.Product
import java.text.Normalizer
import javax.inject.Inject

class SearchProductsUseCase @Inject constructor() {

    // Todos los términos deben aparecer en código, nombre, marca, línea o categoría
    operator fun invoke(products: List<Product>, query: String): List<Product> {
        val terms = normalize(query).split(" ").filter { it.isNotBlank() }
        if (terms.isEmpty()) return products
        return products.filter { p ->
            val haystack = normalize(
                "${p.code} ${p.name} ${p.brand} ${p.line.displayName} ${p.category}"
            )
            terms.all { it in haystack }
        }
    }

    // Ignora mayúsculas y tildes: "griferia" encuentra "Grifería"
    private fun normalize(text: String): String =
        Normalizer.normalize(text.trim().lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
}