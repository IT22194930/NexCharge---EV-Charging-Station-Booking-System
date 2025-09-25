package com.evcharging.evchargingapp.data.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitInstance {

    //  Replace "API_BASE_URL_HERE" with actual API base URL
    private const val BASE_URL = "http://10.88.147.203/EVChargingAPI/api/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY // Logs request and response bodies. Use .NONE for production.
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS) // Optional: Set connection timeout
        .readTimeout(30, TimeUnit.SECONDS)    // Optional: Set read timeout
        .writeTimeout(30, TimeUnit.SECONDS)   // Optional: Set write timeout
        .build()

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
