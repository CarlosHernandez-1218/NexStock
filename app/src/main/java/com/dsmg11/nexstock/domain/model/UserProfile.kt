package com.dsmg11.nexstock.domain.model

data class UserProfile(
    val uid: String,
    val email: String,
    val fullName: String = "",
    val phone: String = "",
    val role: UserRole = UserRole.ALMACENERO,
    val assignedLines: List<ProductLine> = emptyList(),
    val photoUrl: String? = null
)