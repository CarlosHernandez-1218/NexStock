package com.dsmg11.nexstock.domain.usecase.master

import com.dsmg11.nexstock.domain.model.MasterItem
import com.dsmg11.nexstock.domain.model.MasterType
import com.dsmg11.nexstock.domain.repository.MasterDataRepository
import java.text.Normalizer
import javax.inject.Inject

class AddMasterItemUseCase @Inject constructor(
    private val repository: MasterDataRepository
) {
    suspend operator fun invoke(
        type: MasterType,
        name: String,
        existing: List<MasterItem>,
        parentId: String? = null
    ): Result<Unit> {
        val clean = name.trim().replace(Regex("\\s+"), " ")
        if (clean.length < 2) return fail("Ingresa un nombre de al menos 2 caracteres")
        if (type == MasterType.CATEGORIAS && parentId == null) return fail("Selecciona la línea")

        // "Grifería" y "griferia" se consideran el mismo dato
        val duplicated = existing.any { it.parentId == parentId && normalize(it.name) == normalize(clean) }
        if (duplicated) return fail("Ya existe \"$clean\"")

        // Las líneas usan un código legible como ID (ej. "Grifería" → GRIFERIA)
        val id = if (type == MasterType.LINEAS) toCode(clean) else null
        if (id != null && existing.any { it.id == id }) return fail("Ya existe una línea con el código $id")

        return repository.add(type, id, clean, parentId)
    }

    private fun fail(message: String): Result<Unit> = Result.failure(IllegalArgumentException(message))

    private fun normalize(text: String): String =
        Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")

    private fun toCode(text: String): String =
        normalize(text).uppercase().replace(Regex("[^A-Z0-9]+"), "_").trim('_')
}