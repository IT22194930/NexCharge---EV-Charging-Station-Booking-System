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
import kotlinx.coroutines.launch
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var userDao: UserDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        // Corrected ID references for EditTexts
        val nic = binding.editTextLoginNic.text.toString().trim()
        val password = binding.editTextLoginPassword.text.toString().trim()

        if (nic.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter NIC and password", Toast.LENGTH_SHORT).show()
            return
        }

        // Corrected ProgressBar ID and enabled visibility
        binding.progressBarLogin.visibility = View.VISIBLE
        binding.buttonLogin.isEnabled = false

        lifecycleScope.launch {
            try {
                val response = RetrofitInstance.api.login(LoginRequest(nic, password))

                if (response.isSuccessful) {
                    val loginResponse = response.body()

                    if (loginResponse?.token != null && loginResponse.token.isNotEmpty()) {
                        val tokenString = loginResponse.token
                        Toast.makeText(applicationContext, "Login Successful!", Toast.LENGTH_LONG).show()

                        saveAuthToken(tokenString)

                        Log.d("LoginActivity", "Login successful. Token: $tokenString")

                        // Decode token and navigate based on role
                        val userRole = getRoleFromToken(tokenString)
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
                            errorMessage = errorJson.optString("message", "Error (${response.code()}): Invalid credentials or server error.")
                        } else {
                            errorMessage = "Login failed. Error code: ${response.code()} ${response.message()}"
                        }
                    } catch (e: Exception) {
                        Log.e("LoginActivity", "Error parsing error body: ${e.message}")
                        errorMessage = "Login failed. Error code: ${response.code()}"
                    }
                    Toast.makeText(applicationContext, errorMessage, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("LoginActivity", "Login exception: ${e.message}", e)
                Toast.makeText(applicationContext, "Login error: Check network connection.", Toast.LENGTH_LONG).show()
            } finally {
                binding.progressBarLogin.visibility = View.GONE
                binding.buttonLogin.isEnabled = true
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
            // The claim name for role in your .NET code is ClaimTypes.Role,
            // which translates to "http://schemas.microsoft.com/ws/2008/06/identity/claims/role"
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

            // Add other roles here if needed, e.g., "BACKOFFICE"
            else -> {
                Toast.makeText(this, "Role '$role' not recognized or null. Navigating to default home.", Toast.LENGTH_LONG).show()
                Intent(this, HomeActivity::class.java) // Fallback activity
            }
        }
        startActivity(intent)
        finish() // Close LoginActivity so user can't go back
    }
}
