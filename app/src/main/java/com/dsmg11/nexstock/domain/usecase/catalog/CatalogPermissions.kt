package com.dsmg11.nexstock.domain.usecase.catalog

import com.dsmg11.nexstock.domain.model.Product
import com.dsmg11.nexstock.domain.model.ProductLine
import com.dsmg11.nexstock.domain.model.UserProfile
import com.dsmg11.nexstock.domain.model.UserRole

object CatalogPermissions {

    fun canCreate(profile: UserProfile?): Boolean =
        profile?.role == UserRole.ADMINISTRADOR || profile?.role == UserRole.JEFE_LINEA

    fun canEdit(profile: UserProfile?, product: Product): Boolean = when (profile?.role) {
        UserRole.ADMINISTRADOR -> true
        UserRole.JEFE_LINEA -> profile?.assignedLines?.contains(product.line) == true
        else -> false
    }

    // Líneas que el usuario puede elegir en el formulario
    fun allowedLines(profile: UserProfile?): List<ProductLine> = when (profile?.role) {
        UserRole.ADMINISTRADOR -> ProductLine.entries
        UserRole.JEFE_LINEA -> profile?.assignedLines.orEmpty()
        else -> emptyList()
    }
}