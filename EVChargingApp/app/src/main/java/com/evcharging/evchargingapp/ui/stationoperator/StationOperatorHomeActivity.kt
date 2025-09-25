package com.evcharging.evchargingapp.ui.stationoperator

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.evcharging.evchargingapp.databinding.ActivityStationOperatorHomeBinding
import com.evcharging.evchargingapp.ui.LoginActivity

class StationOperatorHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStationOperatorHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityStationOperatorHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.buttonStationOperatorLogout.setOnClickListener {
            performLogout()
        }

        // TODO: Initialize Station Operator specific UI and logic
    }

    private fun performLogout() {
        // Clear saved token
        val sharedPreferences = getSharedPreferences("EVChargingAppPrefs", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            remove("AUTH_TOKEN")
            apply()
        }

        // Navigate to LoginActivity
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Clear back stack
        startActivity(intent)
        finish() // Close this activity
    }
}
