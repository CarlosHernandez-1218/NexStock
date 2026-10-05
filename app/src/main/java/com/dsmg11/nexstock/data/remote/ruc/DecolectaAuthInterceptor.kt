package com.dsmg11.nexstock.data.remote.ruc

import com.dsmg11.nexstock.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class DecolectaAuthInterceptor @Inject constructor() : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {

        val request = chain.request()
            .newBuilder()
            .addHeader(
                "Authorization",
                "Bearer ${BuildConfig.DECOLECTA_API_TOKEN}"
            )
            .addHeader(
                "Accept",
                "application/json"
            )
            .build()

        return chain.proceed(request)
    }
}