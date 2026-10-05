package com.dsmg11.nexstock.data.remote.ruc

import com.google.gson.annotations.SerializedName

data class RucResponseDto(
    @SerializedName("razon_social")
    val razonSocial: String,

    @SerializedName("numero_documento")
    val numeroDocumento: String,

    val estado: String,
    val condicion: String,
    val direccion: String? = null,
    val distrito: String? = null,
    val provincia: String? = null,
    val departamento: String? = null
)