package com.dsmg11.nexstock.data.remote.ruc

import retrofit2.http.GET
import retrofit2.http.Query

interface RucApiService {

    @GET("v1/sunat/ruc")
    suspend fun consultarRuc(
        @Query("numero") ruc: String
    ): RucResponseDto
}