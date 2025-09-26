package com.evcharging.evchargingapp.ui.evowner.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.evcharging.evchargingapp.databinding.FragmentEvownerBookingsBinding

class EVOwnerBookingsFragment : Fragment() {

    private var _binding: FragmentEvownerBookingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEvownerBookingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBookings()
    }

    private fun setupBookings() {
        // Setup RecyclerView for bookings
        binding.recyclerViewBookings.layoutManager = LinearLayoutManager(requireContext())
        
        // Setup tab layout for upcoming and past bookings
        setupTabs()
        
        // Load bookings
        loadBookings()
    }

    private fun setupTabs() {
        // TODO: Setup TabLayout to switch between upcoming and past bookings
        binding.tabLayoutBookings.addTab(binding.tabLayoutBookings.newTab().setText("Upcoming"))
        binding.tabLayoutBookings.addTab(binding.tabLayoutBookings.newTab().setText("Past"))
        
        binding.tabLayoutBookings.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> loadUpcomingBookings()
                    1 -> loadPastBookings()
                }
            }
            
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })
    }

    private fun loadBookings() {
        loadUpcomingBookings()
    }

    private fun loadUpcomingBookings() {
        // TODO: Load upcoming bookings from API
        binding.textViewNoBookings.visibility = View.VISIBLE
        binding.textViewNoBookings.text = "No upcoming bookings"
        binding.recyclerViewBookings.visibility = View.GONE
    }

    private fun loadPastBookings() {
        // TODO: Load charging history from API
        binding.textViewNoBookings.visibility = View.VISIBLE
        binding.textViewNoBookings.text = "No past bookings"
        binding.recyclerViewBookings.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = EVOwnerBookingsFragment()
    }
}