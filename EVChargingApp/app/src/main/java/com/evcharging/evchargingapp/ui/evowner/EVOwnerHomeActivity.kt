package com.evcharging.evchargingapp.ui.evowner

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
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

class EVOwnerHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEvownerHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityEvownerHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupToolbar()
        setupBottomNavigation()
        
        // Load default fragment (Dashboard)
        if (savedInstanceState == null) {
            loadFragment(EVOwnerDashboardFragment.newInstance())
            binding.bottomNavigationEvOwner.selectedItemId = R.id.nav_dashboard
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbarEvOwner)
        supportActionBar?.title = "NexCharge"
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
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainerEvOwner, fragment)
            .commit()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_evowner_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                performLogout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
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
