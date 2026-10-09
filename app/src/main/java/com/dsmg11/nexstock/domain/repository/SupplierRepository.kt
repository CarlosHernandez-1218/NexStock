package com.dsmg11.nexstock.domain.repository

import com.dsmg11.nexstock.domain.model.RucInfo
import com.dsmg11.nexstock.domain.model.Supplier

interface SupplierRepository {

    suspend fun consultarRuc(ruc: String): Result<RucInfo>

    suspend fun existeProveedor(ruc: String): Result<Boolean>

    suspend fun registrarProveedor(proveedor: Supplier): Result<Unit>
    fun observarProveedores(): kotlinx.coroutines.flow.Flow<List<Supplier>>
}