package com.dsmg11.nexstock.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dsmg11.nexstock.domain.model.ProductLine
import com.dsmg11.nexstock.domain.model.UserProfile
import com.dsmg11.nexstock.domain.model.UserRole

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val uid: String,
    val email: String,
    val fullName: String,
    val phone: String,
    val role: String,
    val assignedLines: String,   // Guardado como texto: "PISOS,PINTURAS"
    val photoUrl: String?
)

fun UserProfileEntity.toDomain() = UserProfile(
    uid = uid,
    email = email,
    fullName = fullName,
    phone = phone,
    role = UserRole.fromName(role),
    assignedLines = assignedLines.split(",").mapNotNull { ProductLine.fromName(it) },
    photoUrl = photoUrl
)

fun UserProfile.toEntity() = UserProfileEntity(
    uid = uid,
    email = email,
    fullName = fullName,
    phone = phone,
    role = role.name,
    assignedLines = assignedLines.joinToString(",") { it.name },
    photoUrl = photoUrl
)