package com.dsmg11.nexstock.core.di

import com.dsmg11.nexstock.data.remote.ruc.DecolectaAuthInterceptor
import com.dsmg11.nexstock.data.remote.ruc.RucApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val DECOLECTA_BASE_URL =
        "https://api.decolecta.com/"

    @Provides
    @Singleton
    fun provideDecolectaOkHttpClient(
        authInterceptor: DecolectaAuthInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(DECOLECTA_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideRucApiService(
        retrofit: Retrofit
    ): RucApiService {
        return retrofit.create(RucApiService::class.java)
    }
}