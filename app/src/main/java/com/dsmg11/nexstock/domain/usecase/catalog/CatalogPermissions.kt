package com.dsmg11.nexstock.domain.usecase.catalog

import com.dsmg11.nexstock.domain.model.Product
import com.dsmg11.nexstock.domain.model.ProductLine
import com.dsmg11.nexstock.domain.model.UserProfile
import com.dsmg11.nexstock.domain.model.UserRole
import com.dsmg11.nexstock.domain.model.sameAs

object CatalogPermissions {

    fun canCreate(profile: UserProfile?): Boolean =
        profile?.role == UserRole.ADMINISTRADOR || profile?.role == UserRole.JEFE_LINEA

    fun canEdit(profile: UserProfile?, product: Product): Boolean = when (profile?.role) {
        UserRole.ADMINISTRADOR -> true
        UserRole.JEFE_LINEA -> profile?.assignedLines.orEmpty().any { it.sameAs(product.line) }
        else -> false
    }

    // Líneas que el usuario puede elegir en el formulario (allLines viene de product_lines)
    fun allowedLines(profile: UserProfile?, allLines: List<ProductLine>): List<ProductLine> =
        when (profile?.role) {
            UserRole.ADMINISTRADOR -> allLines
            UserRole.JEFE_LINEA -> profile?.assignedLines.orEmpty().map { assigned ->
                allLines.firstOrNull { it.sameAs(assigned) } ?: assigned
            }
            else -> emptyList()
        }

    // En Stock, el Jefe de Línea solo ve sus líneas; Administrador y Almacenero ven todo
    fun visibleProducts(profile: UserProfile?, products: List<Product>): List<Product> {
        if (profile?.role != UserRole.JEFE_LINEA) return products
        val codes = profile?.assignedLines.orEmpty().map { it.name }.toSet()
        return products.filter { it.line.name in codes }
    }
}