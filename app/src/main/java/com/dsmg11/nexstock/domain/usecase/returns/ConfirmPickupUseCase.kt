package com.dsmg11.nexstock.domain.usecase.returns

import com.dsmg11.nexstock.domain.model.SupplierReturn
import com.dsmg11.nexstock.domain.model.UserProfile
import com.dsmg11.nexstock.domain.repository.SupplierReturnRepository
import javax.inject.Inject

// MOV-64: el proveedor recogió la mercadería → Recogida y se descuenta del inventario
class ConfirmPickupUseCase @Inject constructor(
    private val repository: SupplierReturnRepository
) {
    suspend operator fun invoke(profile: UserProfile?, item: SupplierReturn): Result<Unit> {
        if (profile == null || !ReturnPermissions.canConfirmPickup(profile, item)) {
            return Result.failure(
                IllegalStateException("Solo el Jefe de Línea responsable puede confirmar el recojo")
            )
        }
        return repository.confirmPickup(item.id)
    }
}