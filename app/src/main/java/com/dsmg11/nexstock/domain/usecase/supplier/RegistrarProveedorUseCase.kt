package com.dsmg11.nexstock.domain.usecase.supplier

import com.dsmg11.nexstock.domain.model.Supplier
import com.dsmg11.nexstock.domain.repository.SupplierRepository
import javax.inject.Inject

class RegistrarProveedorUseCase @Inject constructor(
    private val supplierRepository: SupplierRepository
) {

    suspend operator fun invoke(
        proveedor: Supplier
    ): Result<Unit> {

        if (proveedor.ruc.length != 11 ||
            !proveedor.ruc.all { it.isDigit() }
        ) {
            return Result.failure(
                IllegalArgumentException(
                    "El RUC ingresado no es válido"
                )
            )
        }

        if (proveedor.razonSocial.isBlank()) {
            return Result.failure(
                IllegalArgumentException(
                    "Debe consultar y validar el RUC antes de registrar"
                )
            )
        }

        if (proveedor.contacto.isBlank()) {
            return Result.failure(
                IllegalArgumentException(
                    "Ingrese el nombre del contacto"
                )
            )
        }

        val resultadoExiste =
            supplierRepository.existeProveedor(proveedor.ruc)

        if (resultadoExiste.isFailure) {
            return Result.failure(
                resultadoExiste.exceptionOrNull()
                    ?: IllegalStateException(
                        "No se pudo verificar el proveedor"
                    )
            )
        }

        if (resultadoExiste.getOrDefault(false)) {
            return Result.failure(
                IllegalArgumentException(
                    "El proveedor ya se encuentra registrado"
                )
            )
        }

        return supplierRepository.registrarProveedor(proveedor)
    }
}