package com.dsmg11.nexstock.domain.usecase.reception

import com.dsmg11.nexstock.domain.repository.ProductRepository
import javax.inject.Inject

// MOV-48: código del producto que tiene ese código de barras (busca en Room, funciona sin internet)
class FindProductCodeByBarcodeUseCase @Inject constructor(
    private val repository: ProductRepository
) {
    suspend operator fun invoke(barcode: String): String? =
        repository.findBarcodeOwner(barcode.trim(), exceptCode = "")
}
