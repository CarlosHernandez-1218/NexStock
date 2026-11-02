package com.dsmg11.nexstock.domain.usecase.reception

import com.dsmg11.nexstock.domain.model.Reception
import com.dsmg11.nexstock.domain.repository.ReceptionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

// MOV-51: recepciones que esperan conexión para subirse
class ObservePendingReceptionsUseCase @Inject constructor(
    private val repository: ReceptionRepository
) {
    operator fun invoke(): Flow<List<Reception>> = repository.observePending()
}
