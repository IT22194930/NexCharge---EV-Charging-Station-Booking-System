package com.evcharging.evchargingapp.utils

import android.content.Context
import android.util.Log
import com.auth0.android.jwt.JWT

object TokenUtils {
    private const val PREFS_NAME = "EVChargingAppPrefs"
    private const val TOKEN_KEY = "AUTH_TOKEN"
    
    // Microsoft JWT claim names
    private const val ROLE_CLAIM = "http://schemas.microsoft.com/ws/2008/06/identity/claims/role"
    private const val NAME_CLAIM = "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name"
    private const val NAMEIDENTIFIER_CLAIM = "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier"

    private val NIC_CLAIM_ALTERNATIVES = listOf(
        "nic",
        "sub",
        "unique_name",
        "nameid",
        NAME_CLAIM,
        NAMEIDENTIFIER_CLAIM
    )

    fun saveToken(context: Context, token: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(TOKEN_KEY, token).apply()
        Log.d("TokenUtils", "Token saved successfully")
    }

    fun getToken(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(TOKEN_KEY, null)
    }

    fun clearToken(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(TOKEN_KEY).apply()
        Log.d("TokenUtils", "Token cleared")
    }

    fun getCurrentUserNic(context: Context): String? {
        return try {
            val token = getToken(context) ?: return null
            val jwt = JWT(token)
            
            // Try the Microsoft name claim first (this is what ClaimTypes.Name maps to)
            val userNic = jwt.getClaim(NAME_CLAIM).asString()
            
            if (!userNic.isNullOrEmpty()) {
                Log.d("TokenUtils", "Found user NIC: $userNic")
                return userNic
            }
            
            // Fallback to other claim names if needed
            for (claimName in NIC_CLAIM_ALTERNATIVES) {
                val value = jwt.getClaim(claimName).asString()
                if (!value.isNullOrEmpty()) {
                    Log.d("TokenUtils", "Found user identifier in claim '$claimName': $value")
                    return value
                }
            }
            
            Log.w("TokenUtils", "No user identifier found in token claims")
            null
        } catch (e: Exception) {
            Log.e("TokenUtils", "Error extracting user NIC from token", e)
            null
        }
    }

    fun getUserRole(context: Context): String? {
        return try {
            val token = getToken(context) ?: return null
            val jwt = JWT(token)
            
            val role = jwt.getClaim(ROLE_CLAIM).asString()
            Log.d("TokenUtils", "User role from token: $role")
            role
        } catch (e: Exception) {
            Log.e("TokenUtils", "Error extracting user role from token", e)
            null
        }
    }

    fun isTokenValid(context: Context): Boolean {
        return try {
            val token = getToken(context) ?: return false
            val jwt = JWT(token)
            val isValid = !jwt.isExpired(10) // Check if token expires within 10 seconds
            Log.d("TokenUtils", "Token validity: $isValid")
            isValid
        } catch (e: Exception) {
            Log.e("TokenUtils", "Error checking token validity", e)
            false
        }
    }
    
    fun debugTokenClaims(context: Context) {
        try {
            val token = getToken(context)
            if (token != null) {
                val jwt = JWT(token)
                Log.d("TokenUtils", "=== Token Claims Debug ===")
                jwt.claims.forEach { (key, value) ->
                    Log.d("TokenUtils", "$key: ${value.asString()}")
                }
                Log.d("TokenUtils", "=== End Token Claims ===")
            } else {
                Log.d("TokenUtils", "No token found")
            }
        } catch (e: Exception) {
            Log.e("TokenUtils", "Error debugging token claims", e)
        }
    }
    
    // Helper function to test API connectivity
    fun testApiConnectivity(context: Context) {
        Log.d("TokenUtils", "Testing API connectivity...")
        try {
            val token = getToken(context)
            Log.d("TokenUtils", "Token available: ${token != null}")
            
            if (token != null) {
                val jwt = JWT(token)
                val isExpired = jwt.isExpired(10)
                Log.d("TokenUtils", "Token expired: $isExpired")
            }
        } catch (e: Exception) {
            Log.e("TokenUtils", "Error testing API connectivity", e)
        }
    }
}
