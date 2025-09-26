package com.evcharging.evchargingapp.ui.evowner.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.evcharging.evchargingapp.databinding.FragmentEvownerReservationsBinding
import com.evcharging.evchargingapp.data.network.RetrofitInstance
import com.evcharging.evchargingapp.data.model.Station
import com.evcharging.evchargingapp.data.model.StationCreateRequest
import com.evcharging.evchargingapp.data.model.StationUpdateRequest
import com.evcharging.evchargingapp.ui.evowner.adapters.ReservationAdapter
import com.evcharging.evchargingapp.utils.TokenUtils
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class EVOwnerReservationsFragment : Fragment() {

    private var _binding: FragmentEvownerReservationsBinding? = null
    private val binding get() = _binding!!
    private val apiService by lazy { RetrofitInstance.createApiService(requireContext()) }
    private lateinit var reservationAdapter: ReservationAdapter
    private var allStations = listOf<Station>()
    private var filteredStations = listOf<Station>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEvownerReservationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSearchFunctionality()
        setupClickListeners()
        loadStations()
    }

    private fun setupRecyclerView() {
        reservationAdapter = ReservationAdapter(
            onReservationClick = { station ->
                showStationDetails(station)
            },
            onModifyClick = { station ->
                showUpdateStationDialog(station)
            },
            onDeleteClick = { station ->
                confirmDeleteStation(station)
            }
        )
        
        binding.recyclerViewReservations.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = reservationAdapter
        }
    }

    private fun setupSearchFunctionality() {
        binding.editTextSearch.addTextChangedListener { text ->
            filterStations(text.toString())
        }
    }

    private fun setupClickListeners() {
        binding.buttonCreateReservation.setOnClickListener {
            showCreateStationDialog()
        }
        
        binding.fabCreateReservation.setOnClickListener {
            showCreateStationDialog()
        }
    }

    private fun loadStations() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                showLoading(true)
                val userNic = TokenUtils.getCurrentUserNic(requireContext())
                
                if (userNic != null) {
                    val response = apiService.getStationsByOwner(userNic)
                    
                    // Check if fragment is still attached before updating UI
                    if (!isAdded || _binding == null) return@launch
                    
                    if (response.isSuccessful && response.body() != null) {
                        allStations = response.body()!!
                        filteredStations = allStations
                        updateUI()
                    } else {
                        showError("Failed to load stations")
                        showEmptyState()
                    }
                } else {
                    showError("User not authenticated")
                    showEmptyState()
                }
            } catch (e: Exception) {
                showError("Network error: ${e.message}")
                showEmptyState()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun filterStations(query: String) {
        filteredStations = if (query.isEmpty()) {
            allStations
        } else {
            allStations.filter { station ->
                station.name.contains(query, ignoreCase = true) ||
                station.location.contains(query, ignoreCase = true) ||
                station.chargerType.contains(query, ignoreCase = true)
            }
        }
        updateUI()
    }

    private fun updateUI() {
        if (filteredStations.isEmpty()) {
            showEmptyState()
        } else {
            hideEmptyState()
            reservationAdapter.submitList(filteredStations)
        }
    }

    private fun showCreateStationDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(
            android.R.layout.simple_list_item_1, // We'll use a simple layout for now
            null
        )
        
        // For a proper implementation, you should create a custom dialog layout
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Create New Station")
        builder.setMessage("Station creation dialog coming soon...")
        builder.setPositiveButton("Create") { _, _ ->
            // TODO: Implement proper form dialog
            createSampleStation()
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun createSampleStation() {
        val userNic = TokenUtils.getCurrentUserNic(requireContext())
        if (userNic == null) {
            showError("User not authenticated")
            return
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val request = StationCreateRequest(
                    name = "New Station ${System.currentTimeMillis()}",
                    location = "Sample Location",
                    chargerType = "Type 2",
                    pricePerHour = 25.0
                )
                
                val response = apiService.createStation(request)
                
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Station created successfully", Toast.LENGTH_SHORT).show()
                    loadStations() // Refresh the list
                } else {
                    showError("Failed to create station")
                }
            } catch (e: Exception) {
                showError("Network error: ${e.message}")
            }
        }
    }

    private fun showUpdateStationDialog(station: Station) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Update Station")
        builder.setMessage("Update station: ${station.name}")
        
        builder.setPositiveButton("Update") { _, _ ->
            updateStationStatus(station)
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun updateStationStatus(station: Station) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val newStatus = when (station.status) {
                    "Active" -> "Inactive"
                    "Inactive" -> "Active"
                    else -> "Active"
                }
                
                val request = StationUpdateRequest(
                    name = station.name,
                    location = station.location,
                    chargerType = station.chargerType,
                    pricePerHour = station.pricePerHour,
                    status = newStatus
                )
                
                val response = apiService.updateStation(station.id, request)
                
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Station updated successfully", Toast.LENGTH_SHORT).show()
                    loadStations() // Refresh the list
                } else {
                    showError("Failed to update station")
                }
            } catch (e: Exception) {
                showError("Network error: ${e.message}")
            }
        }
    }

    private fun confirmDeleteStation(station: Station) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Station")
            .setMessage("Are you sure you want to delete ${station.name}?")
            .setPositiveButton("Delete") { _, _ ->
                deleteStation(station.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteStation(stationId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = apiService.deleteStation(stationId)
                
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Station deleted successfully", Toast.LENGTH_SHORT).show()
                    loadStations() // Refresh the list
                } else {
                    showError("Failed to delete station")
                }
            } catch (e: Exception) {
                showError("Network error: ${e.message}")
            }
        }
    }

    private fun showStationDetails(station: Station) {
        Toast.makeText(requireContext(), 
            "Station Details:\n" +
            "Name: ${station.name}\n" +
            "Location: ${station.location}\n" +
            "Type: ${station.chargerType}\n" +
            "Price: $${station.pricePerHour}/hour\n" +
            "Status: ${station.status}", 
            Toast.LENGTH_LONG).show()
    }

    private fun showLoading(isLoading: Boolean) {
        binding.buttonCreateReservation.isEnabled = !isLoading
        binding.fabCreateReservation.isEnabled = !isLoading
    }

    private fun showError(message: String) {
        if (isAdded && context != null) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        }
    }

    private fun showEmptyState() {
        binding.layoutEmptyState.visibility = View.VISIBLE
        binding.recyclerViewReservations.visibility = View.GONE
        binding.textViewNoReservations.text = if (binding.editTextSearch.text?.isNotEmpty() == true) {
            "No stations match your search"
        } else {
            "No stations yet"
        }
    }

    private fun hideEmptyState() {
        binding.layoutEmptyState.visibility = View.GONE
        binding.recyclerViewReservations.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = EVOwnerReservationsFragment()
    }
}