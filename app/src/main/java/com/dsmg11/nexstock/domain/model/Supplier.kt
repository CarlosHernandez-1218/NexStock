package com.dsmg11.nexstock.domain.model

data class Supplier(
    val ruc: String = "",
    val razonSocial: String = "",
    val estadoSunat: String = "",
    val condicionSunat: String = "",
    val direccion: String = "",
    val contacto: String = "",
    val telefono: String = "",
    val correo: String = ""
)