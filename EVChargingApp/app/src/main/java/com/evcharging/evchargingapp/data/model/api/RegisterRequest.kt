package com.evcharging.evchargingapp.data.model.api

import com.google.gson.annotations.SerializedName

data class RegisterRequest(
    @SerializedName("nic") 
    val nic: String,

    @SerializedName("FullName") // Changed to match API expectation (PascalCase)
    val FullName: String,

    @SerializedName("contactNo") 
    val contactNo: String,

    @SerializedName("password") // Ensure this matches the JSON key your API expects
    val password: String,

    @SerializedName("role") // Ensure this matches the JSON key your API expects
    val role: String? // Role can be optional or set by server, adjust as needed
)

