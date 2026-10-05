package com.dsmg11.nexstock.domain.usecase.supplier

import com.dsmg11.nexstock.domain.model.RucInfo
import com.dsmg11.nexstock.domain.repository.SupplierRepository
import javax.inject.Inject

class ConsultarRucUseCase @Inject constructor(
    private val supplierRepository: SupplierRepository
) {

    suspend operator fun invoke(ruc: String): Result<RucInfo> {
        val rucLimpio = ruc.trim()

        if (rucLimpio.isBlank()) {
            return Result.failure(
                IllegalArgumentException("Ingrese un RUC")
            )
        }

        if (rucLimpio.length != 11) {
            return Result.failure(
                IllegalArgumentException(
                    "El RUC debe contener 11 dígitos"
                )
            )
        }

        if (!rucLimpio.all { it.isDigit() }) {
            return Result.failure(
                IllegalArgumentException(
                    "El RUC solo debe contener números"
                )
            )
        }

        return supplierRepository.consultarRuc(rucLimpio)
    }
}