package com.dsmg11.nexstock.domain.model

enum class UserRole(val displayName: String) {
    ADMINISTRADOR("Administrador de Tienda"),
    JEFE_LINEA("Jefe de Línea"),
    ALMACENERO("Almacenero");

    companion object {
        // Si el valor guardado no existe o está vacío, se asume el rol con menos permisos
        fun fromName(name: String?): UserRole =
            entries.firstOrNull { it.name == name } ?: ALMACENERO
    }
}