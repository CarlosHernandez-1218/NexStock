package com.dsmg11.nexstock.domain.usecase.returns

import com.dsmg11.nexstock.domain.model.Product
import com.dsmg11.nexstock.domain.model.ReturnStatus
import com.dsmg11.nexstock.domain.model.ReturnType
import com.dsmg11.nexstock.domain.model.Supplier
import com.dsmg11.nexstock.domain.model.SupplierReturn
import com.dsmg11.nexstock.domain.model.UserProfile
import com.dsmg11.nexstock.domain.repository.SupplierReturnRepository
import javax.inject.Inject

// MOV-59: registra una devolución a proveedor (queda en estado Registrada)
class RegisterReturnUseCase @Inject constructor(
    private val repository: SupplierReturnRepository
) {
    suspend operator fun invoke(
        profile: UserProfile?,
        product: Product,
        supplier: Supplier,
        quantity: Int,
        type: ReturnType
    ): Result<Unit> {
        if (profile == null || !ReturnPermissions.canRegister(profile, product.line.name)) {
            return Result.failure(IllegalStateException("No puedes registrar devoluciones de esta línea"))
        }
        if (quantity <= 0) {
            return Result.failure(IllegalArgumentException("La cantidad debe ser mayor a 0"))
        }
        if (quantity > product.stock) {
            return Result.failure(
                IllegalArgumentException("Solo hay ${product.stock} ${product.unit} en stock")
            )
        }
        return repository.register(
            SupplierReturn(
                productCode = product.code,
                productName = product.name,
                line = product.line.name,
                unit = product.unit,
                quantity = quantity,
                supplierRuc = supplier.ruc,
                supplierName = supplier.razonSocial,
                type = type,
                status = ReturnStatus.REGISTRADA,
                createdBy = profile.uid,
                createdByName = profile.fullName.ifBlank { profile.email },
                createdAt = System.currentTimeMillis()
            )
        )
    }
}