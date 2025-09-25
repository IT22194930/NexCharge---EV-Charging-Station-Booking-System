package com.evcharging.evchargingapp.data.network

import com.evcharging.evchargingapp.data.model.api.LoginRequest
import com.evcharging.evchargingapp.data.model.api.LoginResponse
import com.evcharging.evchargingapp.data.model.api.RegisterRequest
import com.evcharging.evchargingapp.data.model.api.RegisterApiResponse // Added import for new response type
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/register") // Please verify this is your correct registration API endpoint
    suspend fun registerUser(@Body registerRequest: RegisterRequest): Response<RegisterApiResponse> // Changed to RegisterApiResponse

}
