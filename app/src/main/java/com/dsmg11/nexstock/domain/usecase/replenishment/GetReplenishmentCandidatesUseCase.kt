package com.dsmg11.nexstock.domain.usecase.replenishment

import com.dsmg11.nexstock.domain.model.Product
import com.dsmg11.nexstock.domain.model.ProductStatus
import com.dsmg11.nexstock.domain.model.StockLevel
import com.dsmg11.nexstock.domain.model.UserProfile
import com.dsmg11.nexstock.domain.model.stockLevel
import com.dsmg11.nexstock.domain.usecase.catalog.CatalogPermissions
import javax.inject.Inject

// Candidatos de reposición: productos activos de las líneas del usuario
// que están en stock bajo o agotado (stock <= mínimo)
class GetReplenishmentCandidatesUseCase @Inject constructor() {

    operator fun invoke(profile: UserProfile?, products: List<Product>): List<Product> =
        CatalogPermissions.visibleProducts(profile, products)
            .filter { it.status == ProductStatus.ACTIVO && it.stockLevel != StockLevel.NORMAL }
            .sortedWith(
                compareByDescending<Product> { it.stockLevel }   // AGOTADO primero
                    .thenBy { it.stock - it.minStock }           // mayor faltante primero
                    .thenBy { it.name }
            )
}