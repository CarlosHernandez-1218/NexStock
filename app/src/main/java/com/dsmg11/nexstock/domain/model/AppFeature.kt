package com.dsmg11.nexstock.domain.model

enum class AppFeature(
    val title: String,
    val description: String,
    val sprint: Int,
    val allowedRoles: Set<UserRole>
) {
    CATALOGO("Catálogo", "Productos y proveedores", 1, UserRole.entries.toSet()),
    STOCK("Stock", "Consulta de existencias", 1, UserRole.entries.toSet()),
    ORDENES_COMPRA("Órdenes de compra", "Abastecimiento y seguimiento", 2,
        setOf(UserRole.ADMINISTRADOR, UserRole.JEFE_LINEA)),
    RECEPCION("Recepción", "Ingreso de mercadería", 2,
        setOf(UserRole.ADMINISTRADOR, UserRole.ALMACENERO)),
    DEVOLUCIONES("Devoluciones", "Devoluciones a proveedor", 2,
        setOf(UserRole.ADMINISTRADOR, UserRole.JEFE_LINEA)),
    DASHBOARD("Dashboard", "Indicadores logísticos", 2,
        setOf(UserRole.ADMINISTRADOR)),
    ASISTENTE("Asistente IA", "Consultas por texto y voz", 3, UserRole.entries.toSet()),
    USUARIOS("Usuarios", "Roles y líneas asignadas", 1,
        setOf(UserRole.ADMINISTRADOR))
}