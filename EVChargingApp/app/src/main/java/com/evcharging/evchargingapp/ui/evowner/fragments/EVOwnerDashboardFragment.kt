package com.evcharging.evchargingapp.ui.evowner.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.evcharging.evchargingapp.databinding.FragmentEvownerDashboardBinding

class EVOwnerDashboardFragment : Fragment() {

    private var _binding: FragmentEvownerDashboardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEvownerDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDashboard()
    }

    private fun setupDashboard() {
        // TODO: Setup dashboard UI elements
        // - Display total stations owned
        // - Show recent bookings statistics
        // - Display revenue summary
        // - Show station status overview
        binding.textViewTotalStations.text = "Total Stations: 0"
        binding.textViewActiveBookings.text = "Active Bookings: 0"
        binding.textViewTotalRevenue.text = "Total Revenue: $0.00"
        binding.textViewStationStatus.text = "All Stations Online: 0/0"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = EVOwnerDashboardFragment()
    }
}