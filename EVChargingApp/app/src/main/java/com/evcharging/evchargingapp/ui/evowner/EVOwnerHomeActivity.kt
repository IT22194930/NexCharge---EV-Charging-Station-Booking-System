package com.evcharging.evchargingapp.ui.evowner

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.evcharging.evchargingapp.R
import com.evcharging.evchargingapp.databinding.ActivityEvownerHomeBinding
import com.evcharging.evchargingapp.ui.LoginActivity
import com.evcharging.evchargingapp.ui.evowner.fragments.EVOwnerBookingsFragment
import com.evcharging.evchargingapp.ui.evowner.fragments.EVOwnerDashboardFragment
import com.evcharging.evchargingapp.ui.evowner.fragments.EVOwnerProfileFragment
import com.evcharging.evchargingapp.ui.evowner.fragments.EVOwnerReservationsFragment
import com.evcharging.evchargingapp.utils.LoadingManager
import com.evcharging.evchargingapp.utils.ThemeManager
import com.evcharging.evchargingapp.utils.safeLoadFragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class EVOwnerHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEvownerHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize theme before setting content view
        val savedTheme = ThemeManager.getSavedTheme(this)
        ThemeManager.applyTheme(savedTheme)
        
        enableEdgeToEdge()
        binding = ActivityEvownerHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupBottomNavigation()
        
        // Show loading screen while initializing
        showInitialLoading()
    }

    private fun showInitialLoading() {
        LoadingManager.show(this, "Welcome to NexCharge")
        
        lifecycleScope.launch {
            // Simulate initialization time
            delay(1500)
            
            // Check if activity is still alive and not finishing
            if (!isFinishing && !isDestroyed) {
                // Load default fragment (Dashboard)
                loadFragment(EVOwnerDashboardFragment.newInstance())
                binding.bottomNavigationEvOwner.selectedItemId = R.id.nav_dashboard
                
                // Hide loading
                LoadingManager.dismiss()
            }
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigationEvOwner.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    loadFragment(EVOwnerDashboardFragment.newInstance())
                    true
                }
                R.id.nav_reservations -> {
                    loadFragment(EVOwnerReservationsFragment.newInstance())
                    true
                }
                R.id.nav_bookings -> {
                    loadFragment(EVOwnerBookingsFragment.newInstance())
                    true
                }
                R.id.nav_profile -> {
                    loadFragment(EVOwnerProfileFragment.newInstance())
                    true
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        safeLoadFragment(R.id.fragmentContainerEvOwner, fragment)
    }

    // Public method to switch tabs from fragments
    fun switchToTab(tabId: Int) {
        binding.bottomNavigationEvOwner.selectedItemId = tabId
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
