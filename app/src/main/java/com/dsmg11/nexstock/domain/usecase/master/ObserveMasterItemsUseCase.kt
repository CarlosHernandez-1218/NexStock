package com.dsmg11.nexstock.domain.usecase.master

import com.dsmg11.nexstock.domain.model.MasterItem
import com.dsmg11.nexstock.domain.model.MasterType
import com.dsmg11.nexstock.domain.repository.MasterDataRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ObserveMasterItemsUseCase @Inject constructor(
    private val repository: MasterDataRepository
) {
    operator fun invoke(type: MasterType, onlyActive: Boolean = false): Flow<List<MasterItem>> =
        repository.observe(type).map { items ->
            if (onlyActive) items.filter { it.active } else items
        }
}