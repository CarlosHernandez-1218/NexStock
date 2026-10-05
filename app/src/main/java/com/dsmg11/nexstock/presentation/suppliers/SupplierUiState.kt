package com.dsmg11.nexstock.presentation.suppliers

data class SupplierUiState(
    val ruc: String = "",
    val razonSocial: String = "",
    val estado: String = "",
    val condicion: String = "",
    val direccion: String = "",
    val contacto: String = "",
    val telefono: String = "",
    val correo: String = "",

    val rucValidado: Boolean = false,
    val isConsultandoRuc: Boolean = false,
    val isRegistrando: Boolean = false,

    val mensajeUsuario: String? = null,
    val mensajeEsError: Boolean = false
)