package com.dsmg11.nexstock.domain.usecase.catalog

import com.dsmg11.nexstock.domain.model.Product
import com.dsmg11.nexstock.domain.model.ProductLine
import com.dsmg11.nexstock.domain.model.RotationLevel
import javax.inject.Inject

class LoadSampleProductsUseCase @Inject constructor(
    private val saveProduct: SaveProductUseCase
) {
    suspend operator fun invoke(): Result<Int> = runCatching {
        SAMPLE_PRODUCTS.forEach { saveProduct(it, isNew = false).getOrThrow() }
        SAMPLE_PRODUCTS.size
    }
}

private val SAMPLE_PRODUCTS = listOf(
    Product("PIS-0001", "Porcelanato marmolizado 60x60", "Celima", ProductLine.PISOS,
        "Porcelanato", "m²", 54.90, 40, stock = 120, rotation = RotationLevel.ALTA),
    Product("SAN-0001", "Inodoro one piece", "Trebol", ProductLine.SANITARIOS,
        "Inodoros", "unidad", 389.00, 5, stock = 12, rotation = RotationLevel.MEDIA),
    Product("GRI-0001", "Mezcladora monocomando para lavatorio", "Vainsa", ProductLine.GRIFERIA,
        "Mezcladoras", "unidad", 159.90, 8, stock = 6, rotation = RotationLevel.MEDIA),
    Product("PIN-0001", "Pintura látex supermate blanco", "CPP", ProductLine.PINTURAS,
        "Látex", "galón", 45.50, 20, stock = 55, rotation = RotationLevel.ALTA),
    Product("MAT-0001", "Cemento Portland Tipo I 42.5 kg", "Sol", ProductLine.MATERIALES_CONSTRUCCION,
        "Cemento", "bolsa", 29.90, 100, stock = 300, rotation = RotationLevel.ALTA),
    Product("FER-0001", "Martillo de uña 16 oz", "Truper", ProductLine.FERRETERIA,
        "Herramientas manuales", "unidad", 32.00, 10, stock = 0, rotation = RotationLevel.BAJA)
)