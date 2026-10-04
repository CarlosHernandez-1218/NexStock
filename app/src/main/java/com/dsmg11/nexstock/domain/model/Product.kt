package com.dsmg11.nexstock.domain.model

data class Product(
    val code: String,                 // SKU interno, ej. "PIS-0001"
    val name: String,
    val brand: String,
    val line: ProductLine,
    val category: String,             // ej. "Porcelanato", "Inodoros"
    val unit: String,                 // ej. "m²", "unidad", "galón"
    val price: Double,
    val minStock: Int,
    val stock: Int = 0,               //  en MOV-26
    val status: ProductStatus = ProductStatus.ACTIVO,
    val rotation: RotationLevel = RotationLevel.MEDIA,
    val barcode: String? = null,      //  MOV-21
    val updatedAt: Long = System.currentTimeMillis(),
    val pendingSync: Boolean = false   // true = cambio guardado en el celular, falta subirlo
)