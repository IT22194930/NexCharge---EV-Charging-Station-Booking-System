package com.evcharging.evchargingapp.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.auth0.android.jwt.JWT
import com.evcharging.evchargingapp.R
import com.evcharging.evchargingapp.ui.evowner.EVOwnerHomeActivity
import com.evcharging.evchargingapp.ui.stationoperator.StationOperatorHomeActivity
import com.evcharging.evchargingapp.utils.ThemeManager

class LauncherActivity : AppCompatActivity() {

    private val splashTimeOut: Long = 3000 // 3 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize theme before setting content view
        val savedTheme = ThemeManager.getSavedTheme(this)
        ThemeManager.applyTheme(savedTheme)
        
        setContentView(R.layout.activity_launcher)

        // Delay the navigation to show splash screen for the specified time
        Handler(Looper.getMainLooper()).postDelayed({
            checkUserSession()
        }, splashTimeOut)
    }

    private fun checkUserSession() {
        val sharedPreferences = getSharedPreferences("EVChargingAppPrefs", Context.MODE_PRIVATE)
        val token = sharedPreferences.getString("AUTH_TOKEN", null)

        if (token != null) {
            try {
                val jwt = JWT(token)
                if (jwt.isExpired(0)) {
                    Log.d("LauncherActivity", "Token is expired. Navigating to Login.")
                    clearTokenAndNavigateToLogin()
                } else {
                    Log.d("LauncherActivity", "Token is valid. Navigating to dashboard.")
                    val role = jwt.getClaim("http://schemas.microsoft.com/ws/2008/06/identity/claims/role").asString()
                    navigateToDashboard(role)
                }
            } catch (e: Exception) {
                Log.e("LauncherActivity", "Error decoding token or token invalid: ${e.message}")
                clearTokenAndNavigateToLogin()
            }
        } else {
            Log.d("LauncherActivity", "No token found. Navigating to Login.")
            navigateToLogin()
        }
    }

    private fun navigateToDashboard(role: String?) {
        Log.d("LauncherActivity", "Navigating to dashboard with role: $role")
        val intent = when (role?.uppercase()) {
            "EVOWNER" -> Intent(this, EVOwnerHomeActivity::class.java)
            "OPERATOR" -> Intent(this, StationOperatorHomeActivity::class.java)


            else -> {
                Log.w("LauncherActivity", "Role '$role' not recognized or is null. Defaulting to Login Screen.")
                Intent(this, LoginActivity::class.java)
            }
        }
        startActivity(intent)
        finishAffinity()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finishAffinity()
    }

    private fun clearTokenAndNavigateToLogin() {
        val sharedPreferences = getSharedPreferences("EVChargingAppPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().remove("AUTH_TOKEN").apply()
        Log.d("LauncherActivity", "Token cleared.")
        navigateToLogin()
    }
}
