package com.dsmg11.nexstock.domain.repository

import com.dsmg11.nexstock.domain.model.MasterItem
import com.dsmg11.nexstock.domain.model.MasterType
import kotlinx.coroutines.flow.Flow

interface MasterDataRepository {
    fun observe(type: MasterType): Flow<List<MasterItem>>
    suspend fun add(type: MasterType, id: String?, name: String, parentId: String?): Result<Unit>
    suspend fun setActive(type: MasterType, id: String, active: Boolean): Result<Unit>
}