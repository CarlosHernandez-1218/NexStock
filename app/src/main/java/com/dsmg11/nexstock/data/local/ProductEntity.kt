package com.dsmg11.nexstock.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dsmg11.nexstock.domain.model.Product
import com.dsmg11.nexstock.domain.model.ProductLine
import com.dsmg11.nexstock.domain.model.ProductStatus
import com.dsmg11.nexstock.domain.model.RotationLevel

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val code: String,
    val name: String,
    val brand: String,
    val line: String,
    val category: String,
    val unit: String,
    val price: Double,
    val minStock: Int,
    val stock: Int,
    val status: String,
    val rotation: String,
    val barcode: String?,
    val updatedAt: Long
)

fun ProductEntity.toDomain() = Product(
    code = code,
    name = name,
    brand = brand,
    line = ProductLine.fromName(line) ?: ProductLine.FERRETERIA,
    category = category,
    unit = unit,
    price = price,
    minStock = minStock,
    stock = stock,
    status = ProductStatus.fromName(status),
    rotation = RotationLevel.fromName(rotation),
    barcode = barcode,
    updatedAt = updatedAt
)

fun Product.toEntity() = ProductEntity(
    code = code,
    name = name,
    brand = brand,
    line = line.name,
    category = category,
    unit = unit,
    price = price,
    minStock = minStock,
    stock = stock,
    status = status.name,
    rotation = rotation.name,
    barcode = barcode,
    updatedAt = updatedAt
)