package com.dsmg11.nexstock.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.dsmg11.nexstock.domain.model.Reception
import com.dsmg11.nexstock.domain.model.ReceptionItem
import com.dsmg11.nexstock.domain.model.ReceptionLocation
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "receptions", indices = [Index("orderId")])
data class ReceptionEntity(
    @PrimaryKey val id: String,
    val orderId: String,
    val supplierRuc: String,
    val supplierName: String,
    val itemsJson: String,                 // la lista de productos guardada como JSON
    val receivedBy: String,
    val receivedAt: Long,
    val latitude: Double?,
    val longitude: Double?,
    val pendingSync: Boolean = true
)

private val gson = Gson()
private val itemsType = object : TypeToken<List<ReceptionItem>>() {}.type

fun ReceptionEntity.toDomain() = Reception(
    id = id,
    orderId = orderId,
    supplierRuc = supplierRuc,
    supplierName = supplierName,
    items = gson.fromJson<List<ReceptionItem>>(itemsJson, itemsType) ?: emptyList(),
    receivedBy = receivedBy,
    receivedAt = receivedAt,
    location = if (latitude != null && longitude != null) ReceptionLocation(latitude, longitude) else null,
    pendingSync = pendingSync
)

fun Reception.toEntity() = ReceptionEntity(
    id = id,
    orderId = orderId,
    supplierRuc = supplierRuc,
    supplierName = supplierName,
    itemsJson = gson.toJson(items),
    receivedBy = receivedBy,
    receivedAt = receivedAt,
    latitude = location?.latitude,
    longitude = location?.longitude,
    pendingSync = pendingSync
)
