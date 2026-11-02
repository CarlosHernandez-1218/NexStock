package com.dsmg11.nexstock.domain.repository

import com.dsmg11.nexstock.domain.model.Reception
import kotlinx.coroutines.flow.Flow

interface ReceptionRepository {
    // Guarda la recepción en el celular y programa su subida a Firestore
    suspend fun register(reception: Reception): Result<Unit>
    // Recepciones guardadas en el celular que todavía no se suben
    fun observePending(): Flow<List<Reception>>
}
