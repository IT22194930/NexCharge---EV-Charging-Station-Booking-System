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
import com.evcharging.evchargingapp.data.network.RetrofitInstance
import com.evcharging.evchargingapp.data.UserRole
import com.evcharging.evchargingapp.data.model.api.RegisterRequest
import com.evcharging.evchargingapp.data.model.api.RegisterApiResponse // Changed import from LoginResponse
import com.evcharging.evchargingapp.databinding.ActivityRegistrationBinding
import com.evcharging.evchargingapp.ui.evowner.EVOwnerHomeActivity
import com.evcharging.evchargingapp.ui.stationoperator.StationOperatorHomeActivity
import kotlinx.coroutines.launch
import java.io.IOException

class RegistrationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegistrationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRegistrationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mainLayout = binding.root
        ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.buttonRegister.setOnClickListener {
            registerUser()
        }

        binding.textViewGoToLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun registerUser() {
        val nic = binding.editTextRegisterNic.text.toString().trim()
        val name = binding.editTextRegisterName.text.toString().trim()
        val contactNumber = binding.editTextRegisterContactNumber.text.toString().trim()
        val password = binding.editTextRegisterPassword.text.toString()
        val confirmPassword = binding.editTextRegisterConfirmPassword.text.toString()
        val defaultRole = UserRole.EV_OWNER.name // Or however your API expects the role

        if (nic.isEmpty() || name.isEmpty() || contactNumber.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBarRegister.visibility = View.VISIBLE
        binding.buttonRegister.isEnabled = false

        val registerRequest = RegisterRequest(
            nic = nic,
            fullName = name,
            contactNo = contactNumber,
            password = password,
            role = defaultRole
        )

        lifecycleScope.launch {
            try {
                val response = RetrofitInstance.api.registerUser(registerRequest)

                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    val tokenString = apiResponse?.token
                    val roleFromApiResponse = apiResponse?.role

                    Log.d(
                        "RegistrationActivity",
                        "API Response: NIC: ${apiResponse?.nic}, Name: ${apiResponse?.fullName}, Role: $roleFromApiResponse, Token: $tokenString, Message: ${apiResponse?.message}"
                    )

                    if (tokenString != null && tokenString.isNotEmpty()) {
                        Toast.makeText(
                            applicationContext,
                            apiResponse?.message ?: "Registration Successful! Token received.",
                            Toast.LENGTH_LONG
                        ).show()
                        Log.d(
                            "RegistrationActivity",
                            "Registration successful. Token: $tokenString"
                        )
                        saveAuthToken(tokenString)
                        val userRoleFromToken = getRoleFromToken(tokenString)
                        Log.d("RegistrationActivity", "Role from Token: $userRoleFromToken")
                        navigateToDashboard(userRoleFromToken) // Prioritize role from token if token exists
                        finish()
                    } else if (roleFromApiResponse != null && roleFromApiResponse.isNotEmpty()) {
                        // NO TOKEN, but ROLE is present in API response - WORKAROUND
                        Toast.makeText(
                            applicationContext,
                            apiResponse?.message
                                ?: "Registration successful. Navigating with role from API.",
                            Toast.LENGTH_LONG
                        ).show()
                        Log.w(
                            "RegistrationActivity",
                            "Registration successful but no token. Using role directly from API response: $roleFromApiResponse. User will not be authenticated."
                        )
                        // IMPORTANT: User is NOT authenticated as no token is saved.
                        // Subsequent API calls requiring authentication will fail.
                        navigateToDashboard(roleFromApiResponse)
                        finish()
                    } else {
                        // No token and no role from API response (or response is problematic)
                        val message = apiResponse?.message
                            ?: "Registration failed. Unexpected response from server."
                        Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
                        Log.e(
                            "RegistrationActivity",
                            "Registration successful but no token and no role in API response."
                        )
                        binding.progressBarRegister.visibility = View.GONE
                        binding.buttonRegister.isEnabled = true
                        val intent = Intent(this@RegistrationActivity, LoginActivity::class.java)
                        startActivity(intent)
                        finish()
                    }

                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = if (errorBody.isNullOrEmpty()) {
                        "Registration failed: ${response.code()} ${response.message()}"
                    } else {
                        "Registration failed: $errorBody"
                    }
                    Toast.makeText(applicationContext, errorMessage, Toast.LENGTH_LONG).show()
                    Log.e("RegistrationActivity", "Registration error: $errorMessage")
                    binding.progressBarRegister.visibility = View.GONE
                    binding.buttonRegister.isEnabled = true
                }

            } catch (e: IOException) {
                Toast.makeText(
                    applicationContext,
                    "Registration failed: Network error",
                    Toast.LENGTH_LONG
                ).show()
                Log.e("RegistrationActivity", "Network error: ${e.message}", e)
                binding.progressBarRegister.visibility = View.GONE
                binding.buttonRegister.isEnabled = true
            } catch (e: Exception) { // Catch other potential exceptions
                Toast.makeText(
                    applicationContext,
                    "Registration failed: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                Log.e("RegistrationActivity", "Generic error: ${e.message}", e)
                binding.progressBarRegister.visibility = View.GONE
                binding.buttonRegister.isEnabled = true
            }
        }
    }

    private fun saveAuthToken(token: String) {
        val sharedPreferences = getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().putString("auth_token", token).apply()
        Log.i("RegistrationActivity", "Auth token saved.")
    }

    private fun getRoleFromToken(token: String): String? {
        return try {
            val jwt = JWT(token)
            Log.d("RegistrationActivity", "JWT Decoded. Claims: ${jwt.claims}")
            val roleClaim = jwt.getClaim("role").asString() // Ensure your JWT claim name is 'role'
            Log.d("RegistrationActivity", "Role claim ('role') value from token: $roleClaim")
            roleClaim
        } catch (e: Exception) {
            Log.e("RegistrationActivity", "Error decoding JWT or finding role: ${e.message}")
            null
        }
    }

    private fun navigateToDashboard(role: String?) {
        Log.d("LoginActivity", "Navigating with role: $role")
        val intent = when (role?.uppercase()) {
            "EVOWNER" -> Intent(this, EVOwnerHomeActivity::class.java)
            "OPERATOR" -> Intent(this, StationOperatorHomeActivity::class.java)

            // Add other roles here if needed, e.g., "BACKOFFICE"
            else -> {
                Toast.makeText(
                    this,
                    "Role '$role' not recognized or null. Navigating to default home.",
                    Toast.LENGTH_LONG
                ).show()
                Intent(this, HomeActivity::class.java) // Fallback activity
            }
        }
        startActivity(intent)
        finish() // Close LoginActivity so user can't go back
    }
}