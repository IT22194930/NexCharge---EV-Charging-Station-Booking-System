package com.evcharging.evchargingapp.data.model.api

import com.google.gson.annotations.SerializedName

data class RegisterApiResponse(
    @SerializedName("nic")
    val nic: String?,

    @SerializedName("fullName")
    val fullName: String?,

    @SerializedName("role")
    val role: String?,

    // Field for the token, in case the API provides it in the future
    @SerializedName("token")
    val token: String?,

    // Field for a message, in case the API provides it
    @SerializedName("message")
    val message: String?
)
