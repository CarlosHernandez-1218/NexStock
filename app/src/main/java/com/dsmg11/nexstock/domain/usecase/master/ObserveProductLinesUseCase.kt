package com.dsmg11.nexstock.domain.usecase.master

import com.dsmg11.nexstock.domain.model.MasterType
import com.dsmg11.nexstock.domain.model.ProductLine
import com.dsmg11.nexstock.domain.repository.MasterDataRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ObserveProductLinesUseCase @Inject constructor(
    private val repository: MasterDataRepository
) {
    operator fun invoke(onlyActive: Boolean = true): Flow<List<ProductLine>> =
        repository.observe(MasterType.LINEAS).map { items ->
            items.filter { !onlyActive || it.active }.map { ProductLine(it.id, it.name) }
        }
}