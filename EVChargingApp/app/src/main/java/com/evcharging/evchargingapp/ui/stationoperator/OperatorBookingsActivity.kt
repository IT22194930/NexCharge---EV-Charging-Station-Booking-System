package com.evcharging.evchargingapp.ui.stationoperator

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import com.evcharging.evchargingapp.databinding.ActivityOperatorBookingsBinding
import com.evcharging.evchargingapp.ui.stationoperator.adapters.OperatorBookingsPagerAdapter
import com.google.android.material.tabs.TabLayoutMediator

class OperatorBookingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOperatorBookingsBinding
    private lateinit var pagerAdapter: OperatorBookingsPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityOperatorBookingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupViewPager()
        setupSearchFunctionality()
        setupClickListeners()
    }

    private fun setupViewPager() {
        pagerAdapter = OperatorBookingsPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter
        
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Approved"
                1 -> "Completed"
                else -> ""
            }
        }.attach()
        
        // Set initial tab based on intent extra
        val bookingStatus = intent.getStringExtra("BOOKING_STATUS")
        val initialTab = when (bookingStatus) {
            "Completed" -> 1
            "Approved" -> 0
            else -> 0 // Default to approved tab
        }
        binding.viewPager.setCurrentItem(initialTab, false)
    }

    private fun setupSearchFunctionality() {
        binding.editTextSearch.addTextChangedListener { text ->
            val query = text.toString().trim()
            
            // Update search hint based on input
            binding.textInputLayoutSearch.hint = if (query.isEmpty()) {
                "Search by station, booking ID, or customer..."
            } else {
                "Searching for: \"$query\""
            }
            
            // Filter bookings in both tabs
            pagerAdapter.filterBookingsInAllTabs(query)
        }
    }

    private fun setupClickListeners() {
        // Back button
        binding.buttonBack.setOnClickListener {
            finish()
        }
        
        // Refresh button (optional)
        binding.buttonRefresh?.setOnClickListener {
            // Clear search
            binding.editTextSearch.text?.clear()
            
            // Refresh all tabs
            pagerAdapter.refreshAllTabs()
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning to this activity
        pagerAdapter.refreshAllTabs()
    }
}