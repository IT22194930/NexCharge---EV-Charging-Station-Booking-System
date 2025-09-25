package com.evcharging.evchargingapp.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen // Correct import
import com.auth0.android.jwt.JWT
import com.evcharging.evchargingapp.R
import com.evcharging.evchargingapp.ui.backoffice.BackOfficeHomeActivity
import com.evcharging.evchargingapp.ui.evowner.EVOwnerHomeActivity
import com.evcharging.evchargingapp.ui.stationoperator.StationOperatorHomeActivity

class LauncherActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must be called before super.onCreate() and setContentView()
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Keep the splash screen visible for this Activity if you have background
        // tasks before navigation or setting the content view.
        // splashScreen.setKeepOnScreenCondition { true } // Example: keep on screen
        // If you don't have long-running tasks, you might not need setKeepOnScreenCondition.
        // Once your data is loaded (or decision made), you would set it to false.

        setContentView(R.layout.activity_launcher) // Your existing launcher layout

        checkUserSession()
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
            "BACKOFFICE" -> Intent(this, BackOfficeHomeActivity::class.java)

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
