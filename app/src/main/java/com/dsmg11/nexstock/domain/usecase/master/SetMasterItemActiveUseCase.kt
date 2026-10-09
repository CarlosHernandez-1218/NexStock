package com.dsmg11.nexstock.domain.usecase.master

import com.dsmg11.nexstock.domain.model.MasterType
import com.dsmg11.nexstock.domain.repository.MasterDataRepository
import javax.inject.Inject

class SetMasterItemActiveUseCase @Inject constructor(
    private val repository: MasterDataRepository
) {
    suspend operator fun invoke(type: MasterType, id: String, active: Boolean): Result<Unit> =
        repository.setActive(type, id, active)
}