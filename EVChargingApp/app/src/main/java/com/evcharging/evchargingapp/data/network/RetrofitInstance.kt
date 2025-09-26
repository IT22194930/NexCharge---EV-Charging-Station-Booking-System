package com.evcharging.evchargingapp.data.network

import android.content.Context
import android.util.Log
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitInstance {

    // API Base URLs - try these in order if one doesn't work
    private const val BASE_URL_NETWORK = "http://192.168.1.63/EVChargingAPI/api/"  // Your PC's network IP with IIS
    private const val BASE_URL_EMULATOR = "http://10.0.2.2/EVChargingAPI/api/"     // For Android emulator with IIS
    private const val BASE_URL_LOCALHOST = "http://localhost/EVChargingAPI/api/"    // Localhost fallback with IIS
    
    // Use the network IP as specified
    private val CURRENT_BASE_URL = BASE_URL_NETWORK

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY // Logs request and response bodies. Use .NONE for production.
    }

    // Create auth interceptor to add Authorization header
    private fun createAuthInterceptor(context: Context): Interceptor {
        return Interceptor { chain ->
            val request = chain.request()
            val token = context.getSharedPreferences("EVChargingAppPrefs", Context.MODE_PRIVATE)
                .getString("AUTH_TOKEN", null)

            Log.d("RetrofitInstance", "Making API request to: ${request.url}")
            Log.d("RetrofitInstance", "Token present: ${token != null}")

            val newRequest = if (token != null) {
                request.newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
            } else {
                request
            }

            try {
                val response = chain.proceed(newRequest)
                Log.d("RetrofitInstance", "API response code: ${response.code}")
                response
            } catch (e: Exception) {
                Log.e("RetrofitInstance", "API request failed: ${e.message}", e)
                throw e
            }
        }
    }

    fun createApiService(context: Context): ApiService {
        Log.d("RetrofitInstance", "Creating API service with base URL: $CURRENT_BASE_URL")
        
        val httpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(createAuthInterceptor(context))
            .connectTimeout(60, TimeUnit.SECONDS) // Increased from 30 to 60 seconds
            .readTimeout(60, TimeUnit.SECONDS)    // Increased from 30 to 60 seconds
            .writeTimeout(60, TimeUnit.SECONDS)   // Increased from 30 to 60 seconds
            .callTimeout(120, TimeUnit.SECONDS)   // Added call timeout
            .retryOnConnectionFailure(true)       // Added retry on connection failure
            .build()

        return Retrofit.Builder()
            .baseUrl(CURRENT_BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    // Keep the old api property for backward compatibility, but without auth
    val api: ApiService by lazy {
        val httpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(CURRENT_BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    // New property that requires context for authentication
    val apiService: ApiService
        get() = throw IllegalStateException("Use createApiService(context) instead")
}
