package com.evcharging.evchargingapp.data.model.api

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @SerializedName("token")
    val token: String?,

    @SerializedName("message")
    val message: String?
)

// UserDetails can be kept if your API error responses include it or for other API calls,
// but it's not part of the successful login response based on your logs.
// data class UserDetails(
//     val nic: String?,
//     val name: String?,
//     val role: String?
// )