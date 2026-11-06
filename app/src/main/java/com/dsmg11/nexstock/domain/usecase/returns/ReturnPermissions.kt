package com.dsmg11.nexstock.domain.usecase.returns

import com.dsmg11.nexstock.domain.model.Product
import com.dsmg11.nexstock.domain.model.ProductStatus
import com.dsmg11.nexstock.domain.model.SupplierReturn
import com.dsmg11.nexstock.domain.model.UserProfile
import com.dsmg11.nexstock.domain.model.UserRole
import com.dsmg11.nexstock.domain.usecase.catalog.CatalogPermissions
import com.dsmg11.nexstock.domain.model.ReturnStatus

object ReturnPermissions {

    // Administrador: cualquier producto. Jefe de Línea: solo productos de sus líneas.
    fun canRegister(profile: UserProfile?, lineCode: String): Boolean = when (profile?.role) {
        UserRole.ADMINISTRADOR -> true
        UserRole.JEFE_LINEA -> profile?.assignedLines.orEmpty().any { it.name == lineCode }
        else -> false
    }

    // Productos que se pueden devolver: activos y con stock
    fun registrableProducts(profile: UserProfile?, products: List<Product>): List<Product> =
        CatalogPermissions.visibleProducts(profile, products)
            .filter { canRegister(profile, it.line.name) }
            .filter { it.status == ProductStatus.ACTIVO && it.stock > 0 }
            .sortedBy { it.name }

    // El Jefe de Línea solo ve las devoluciones de sus líneas; el Administrador ve todas
    fun visibleReturns(profile: UserProfile?, returns: List<SupplierReturn>): List<SupplierReturn> =
        when (profile?.role) {
            UserRole.ADMINISTRADOR -> returns
            UserRole.JEFE_LINEA -> {
                val lines = profile?.assignedLines.orEmpty().map { it.name }.toSet()
                returns.filter { it.line in lines }
            }
            else -> emptyList()
        }

    // Solo el Jefe de Línea de esa línea autoriza, y solo una devolución recién registrada
    fun canAuthorize(profile: UserProfile?, item: SupplierReturn): Boolean =
        profile?.role == UserRole.JEFE_LINEA &&
                item.status == ReturnStatus.REGISTRADA &&
                profile?.assignedLines.orEmpty().any { it.name == item.line }

    // Coordinar el recojo con el proveedor: Jefe de Línea de esa línea, con la devolución ya autorizada
    fun canRequestPickup(profile: UserProfile?, item: SupplierReturn): Boolean =
        profile?.role == UserRole.JEFE_LINEA &&
                item.status == ReturnStatus.AUTORIZADA &&
                profile?.assignedLines.orEmpty().any { it.name == item.line }
}