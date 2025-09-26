package com.evcharging.evchargingapp.ui.evowner.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.evcharging.evchargingapp.databinding.FragmentEvownerProfileBinding
import com.evcharging.evchargingapp.ui.LoginActivity

class EVOwnerProfileFragment : Fragment() {

    private var _binding: FragmentEvownerProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEvownerProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupProfile()
    }

    private fun setupProfile() {
        // TODO: Load user profile data from API
        loadProfileData()
        
        // Setup click listeners
        binding.buttonUpdateProfile.setOnClickListener {
            updateProfile()
        }
        
        binding.buttonDeactivateAccount.setOnClickListener {
            showDeactivateConfirmation()
        }
        
        binding.buttonChangePassword.setOnClickListener {
            // TODO: Navigate to change password screen
            Toast.makeText(requireContext(), "Change password functionality coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadProfileData() {
        // TODO: Load profile data from API
        // For now, show placeholder data
        binding.editTextName.setText("John Doe")
        binding.editTextEmail.setText("john.doe@example.com")
        binding.editTextPhone.setText("+1234567890")
        binding.textViewMemberSince.text = "Member since: January 2024"
    }

    private fun updateProfile() {
        val name = binding.editTextName.text.toString().trim()
        val email = binding.editTextEmail.text.toString().trim()
        val phone = binding.editTextPhone.text.toString().trim()
        
        if (name.isEmpty() || email.isEmpty() || phone.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }
        
        // TODO: Update profile via API
        Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
    }

    private fun showDeactivateConfirmation() {
        // TODO: Show confirmation dialog for account deactivation
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Deactivate Account")
            .setMessage("Are you sure you want to deactivate your account? This action cannot be undone.")
            .setPositiveButton("Deactivate") { _, _ ->
                deactivateAccount()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deactivateAccount() {
        // TODO: Deactivate account via API
        Toast.makeText(requireContext(), "Account deactivated", Toast.LENGTH_SHORT).show()
        
        // Clear saved token and navigate to login
        val sharedPreferences = requireContext().getSharedPreferences("EVChargingAppPrefs", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            remove("AUTH_TOKEN")
            apply()
        }
        
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = EVOwnerProfileFragment()
    }
}