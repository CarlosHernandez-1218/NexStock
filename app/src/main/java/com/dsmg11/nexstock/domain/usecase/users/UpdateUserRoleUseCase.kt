package com.dsmg11.nexstock.domain.usecase.users

import com.dsmg11.nexstock.domain.model.ProductLine
import com.dsmg11.nexstock.domain.model.UserRole
import com.dsmg11.nexstock.domain.repository.UserProfileRepository
import javax.inject.Inject

class UpdateUserRoleUseCase @Inject constructor(
    private val repository: UserProfileRepository
) {
    suspend operator fun invoke(
        currentAdminUid: String?,
        targetUid: String,
        role: UserRole,
        lines: List<ProductLine>
    ): Result<Unit> {
        // El Administrador no puede quitarse su propio rol
        if (targetUid == currentAdminUid && role != UserRole.ADMINISTRADOR) {
            return Result.failure(
                IllegalArgumentException("No puedes quitarte el rol de Administrador")
            )
        }
        // Un Jefe de Línea debe tener al menos una línea a cargo
        if (role == UserRole.JEFE_LINEA && lines.isEmpty()) {
            return Result.failure(
                IllegalArgumentException("Asigna al menos una línea al Jefe de Línea")
            )
        }
        // Solo el Jefe de Línea tiene líneas asignadas
        val finalLines = if (role == UserRole.JEFE_LINEA) lines.sortedBy { it.ordinal } else emptyList()

        return repository.updateRoleAndLines(targetUid, role, finalLines)
            .recoverCatching { throw Exception("No se pudo guardar. Revisa tu conexión a internet.") }
    }
}