package com.dsmg11.nexstock.domain.usecase.returns

import com.dsmg11.nexstock.domain.model.ReturnStatus
import com.dsmg11.nexstock.domain.model.SupplierReturn
import com.dsmg11.nexstock.domain.model.UserProfile
import com.dsmg11.nexstock.domain.repository.SupplierReturnRepository
import javax.inject.Inject

// MOV-60: autoriza la devolución. El aviso al Administrador lo envía la Cloud Function
// al detectar el cambio de estado a Autorizada.
class AuthorizeReturnUseCase @Inject constructor(
    private val repository: SupplierReturnRepository
) {
    suspend operator fun invoke(profile: UserProfile?, item: SupplierReturn): Result<Unit> {
        if (profile == null || !ReturnPermissions.canAuthorize(profile, item)) {
            return Result.failure(
                IllegalStateException("Solo el Jefe de Línea responsable puede autorizar esta devolución")
            )
        }
        return repository.moveTo(
            returnId = item.id,
            target = ReturnStatus.AUTORIZADA,
            fields = mapOf(
                "authorizedBy" to profile.uid,
                "authorizedByName" to profile.fullName.ifBlank { profile.email },
                "authorizedAt" to System.currentTimeMillis()
            )
        )
    }
}