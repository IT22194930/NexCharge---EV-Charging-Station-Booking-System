package com.evcharging.evchargingapp.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.auth0.android.jwt.JWT
import com.evcharging.evchargingapp.R
import com.evcharging.evchargingapp.data.model.api.LoginRequest
import com.evcharging.evchargingapp.data.network.RetrofitInstance
import com.evcharging.evchargingapp.databinding.ActivityLoginBinding
import com.evcharging.evchargingapp.db.UserDao
import com.evcharging.evchargingapp.ui.HomeActivity
import com.evcharging.evchargingapp.ui.evowner.EVOwnerHomeActivity
import com.evcharging.evchargingapp.ui.stationoperator.StationOperatorHomeActivity
import com.evcharging.evchargingapp.utils.ThemeManager
import com.evcharging.evchargingapp.utils.LoadingManager
import kotlinx.coroutines.launch
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var userDao: UserDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize theme before setting content view
        val savedTheme = ThemeManager.getSavedTheme(this)
        ThemeManager.applyTheme(savedTheme)
        
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        userDao = UserDao(this) // Initialize UserDao

        binding.buttonLogin.setOnClickListener {
            loginUser()
        }

        // Corrected SAM-construct and ID reference
        binding.textViewGoToRegister.setOnClickListener {
            val intent = Intent(this, RegistrationActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loginUser() {
        // Prevent multiple login attempts
        if (LoadingManager.isShowing()) {
            return
        }
        
        // Corrected ID references for EditTexts
        val nic = binding.editTextLoginNic.text.toString().trim()
        val password = binding.editTextLoginPassword.text.toString().trim()

        Log.d("LoginActivity", "Attempting login for NIC: $nic")
        Log.d("LoginActivity", "Base URL being used: http://192.168.1.63/EVChargingAPI/api/")

        if (nic.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter NIC and password", Toast.LENGTH_SHORT).show()
            return
        }

        // Show loading screen instead of just progress bar
        LoadingManager.show(this, "Signing you in...")
        setLoginButtonLoading(true)

        lifecycleScope.launch {
            try {
                // Update loading message for API call
                LoadingManager.updateMessage("Verifying credentials...")
                
                val response = RetrofitInstance.api.login(LoginRequest(nic, password))

                if (response.isSuccessful) {
                    val loginResponse = response.body()

                    if (loginResponse?.token != null && loginResponse.token.isNotEmpty()) {
                        val tokenString = loginResponse.token
                        
                        // Update loading message
                        LoadingManager.updateMessage("Setting up your account...")
                        
                        saveAuthToken(tokenString)

                        Log.d("LoginActivity", "Login successful. Token: $tokenString")

                        // Decode token and navigate based on role
                        val userRole = getRoleFromToken(tokenString)
                        
                        // Final loading message
                        LoadingManager.updateMessage("Redirecting to dashboard...")
                        
                        Toast.makeText(applicationContext, "Welcome! Redirecting to your dashboard...", Toast.LENGTH_SHORT).show()
                        navigateToDashboard(userRole)

                    } else {
                        val message = "Login failed: Server returned success but no valid token in response."
                        Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
                    }
                } else {
                    var errorMessage = "Login failed"
                    try {
                        val errorBodyString = response.errorBody()?.string()
                        if (!errorBodyString.isNullOrEmpty()) {
                            val errorJson = JSONObject(errorBodyString)
                            val serverMessage = errorJson.optString("message", "")
                            
                            // Check for specific error messages from server
                            errorMessage = when {
                                serverMessage.contains("inactive", ignoreCase = true) || 
                                serverMessage.contains("deactivated", ignoreCase = true) ||
                                serverMessage.contains("disabled", ignoreCase = true) -> {
                                    "Your account has been deactivated. Please contact support to reactivate your account."
                                }
                                serverMessage.contains("suspended", ignoreCase = true) -> {
                                    "Your account has been suspended. Please contact support for assistance."
                                }
                                serverMessage.contains("invalid", ignoreCase = true) && 
                                serverMessage.contains("credentials", ignoreCase = true) -> {
                                    "Invalid credentials. Please check your NIC and password."
                                }
                                serverMessage.contains("not found", ignoreCase = true) -> {
                                    "User not found. Please check your NIC or register a new account."
                                }
                                serverMessage.isNotEmpty() -> serverMessage
                                else -> "Invalid credentials. Please check your NIC and password."
                            }
                        } else {
                            // Handle different HTTP status codes
                            errorMessage = when (response.code()) {
                                401 -> "Invalid credentials. Please check your NIC and password."
                                403 -> "Your account has been deactivated. Please contact support to reactivate your account."
                                400 -> "Invalid request. Please check your input."
                                404 -> "User not found. Please check your NIC or register a new account."
                                500 -> "Server error. Please try again later."
                                else -> "Login failed. Please try again."
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("LoginActivity", "Error parsing error body: ${e.message}")
                        errorMessage = when (response.code()) {
                            401 -> "Invalid credentials. Please check your NIC and password."
                            403 -> "Your account has been deactivated. Please contact support to reactivate your account."
                            400 -> "Invalid request. Please check your input."
                            404 -> "User not found. Please check your NIC or register a new account."
                            500 -> "Server error. Please try again later."
                            else -> "Login failed. Please try again."
                        }
                    }
                    Toast.makeText(applicationContext, errorMessage, Toast.LENGTH_LONG).show()
                }
            } catch (e: java.net.SocketTimeoutException) {
                Log.e("LoginActivity", "Connection timeout: ${e.message}", e)
                val errorMessage = """
                    Connection timeout - Server is taking too long to respond.
                    
                    Please try again or check your internet connection.
                """.trimIndent()
                Toast.makeText(applicationContext, errorMessage, Toast.LENGTH_LONG).show()
            } catch (e: java.net.ConnectException) {
                Log.e("LoginActivity", "Connection failed: ${e.message}", e)
                val errorMessage = """
                    Unable to connect to server.
                    
                    Please check your internet connection and try again.
                """.trimIndent()
                Toast.makeText(applicationContext, errorMessage, Toast.LENGTH_LONG).show()
            } catch (e: java.net.UnknownHostException) {
                Log.e("LoginActivity", "Unknown host: ${e.message}", e)
                Toast.makeText(applicationContext, "Network error: Please check your internet connection and try again.", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Log.e("LoginActivity", "Login exception: ${e.message}", e)
                Toast.makeText(applicationContext, "Login error: Please check your connection and try again.", Toast.LENGTH_LONG).show()
            } finally {
                LoadingManager.dismiss()
                setLoginButtonLoading(false)
            }
        }
    }

    private fun saveAuthToken(token: String) {
        val sharedPreferences = getSharedPreferences("EVChargingAppPrefs", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString("AUTH_TOKEN", token)
            apply()
        }
        Log.d("LoginActivity", "Auth token saved.")
    }

    private fun getRoleFromToken(token: String): String? {
        try {
            val jwt = JWT(token)
            return jwt.getClaim("http://schemas.microsoft.com/ws/2008/06/identity/claims/role").asString()
        } catch (e: Exception) {
            Log.e("LoginActivity", "Failed to decode JWT or get role claim: ${e.message}")
            return null
        }
    }

    private fun navigateToDashboard(role: String?) {
        Log.d("LoginActivity", "Navigating with role: $role")
        val intent = when (role?.uppercase()) {
            "EVOWNER" -> Intent(this, EVOwnerHomeActivity::class.java)
            "OPERATOR" -> Intent(this, StationOperatorHomeActivity::class.java)

            else -> {
                Toast.makeText(this, "Role '$role' not recognized or null. Navigating to default home.", Toast.LENGTH_LONG).show()
                Intent(this, HomeActivity::class.java) // Fallback activity
            }
        }
        startActivity(intent)
        finish() // Close LoginActivity so user can't go back
    }

    private fun setLoginButtonLoading(isLoading: Boolean) {
        binding.buttonLogin.apply {
            isEnabled = !isLoading
            text = if (isLoading) "Signing in..." else "Login"
            if (isLoading) {
                setIconResource(android.R.drawable.ic_popup_sync)
                iconTint = android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.WHITE
                )
            } else {
                setIconResource(R.drawable.ic_person)
                iconTint = android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.WHITE
                )
            }
        }
    }
}
