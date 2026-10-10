package com.dsmg11.nexstock.domain.usecase.catalog

import com.dsmg11.nexstock.domain.model.Product
import com.dsmg11.nexstock.domain.repository.ProductRepository
import javax.inject.Inject

class SaveProductUseCase @Inject constructor(
    private val repository: ProductRepository
) {
    suspend operator fun invoke(product: Product, isNew: Boolean): Result<Unit> {
        val clean = product.copy(
            code = product.code.trim().uppercase(),
            name = product.name.trim(),
            brand = product.brand.trim(),
            category = product.category.trim(),
            unit = product.unit.trim(),
            barcode = product.barcode?.trim()?.ifBlank { null }
        )

        //el código de barras es opcional, pero si se ingresa debe ser válido y único
        val barcode = clean.barcode
        val barcodeError = barcode?.let { BarcodeValidator.error(it) }
        val barcodeOwner = if (barcode != null && barcodeError == null) {
            repository.findBarcodeOwner(barcode, exceptCode = clean.code)
        } else null

        val error = when {
            !CODE_REGEX.matches(clean.code) ->
                "El código debe tener de 3 a 20 caracteres (letras, números o guiones)"
            clean.name.length < 3 -> "El nombre debe tener al menos 3 caracteres"
            clean.brand.isBlank() -> "Ingresa la marca"
            clean.category.isBlank() -> "Ingresa la categoría"
            clean.unit.isBlank() -> "Selecciona la unidad de medida"
            clean.price <= 0.0 -> "El precio debe ser mayor a 0"
            clean.minStock < 0 -> "El stock mínimo no puede ser negativo"
            clean.stock < 0 -> "El stock no puede ser negativo"
            barcodeError != null -> barcodeError
            isNew && repository.existsProduct(clean.code) ->
                "Ya existe un producto con el código ${clean.code}"
            barcodeOwner != null ->
                "El código de barras $barcode ya está asignado al producto $barcodeOwner"
            else -> null
        }
        if (error != null) return Result.failure(IllegalArgumentException(error))

        return repository.saveProduct(clean)
    }

    private companion object {
        val CODE_REGEX = Regex("^[A-Z0-9-]{3,20}$")
    }
}