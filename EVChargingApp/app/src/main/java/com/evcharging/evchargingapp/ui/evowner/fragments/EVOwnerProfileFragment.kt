package com.evcharging.evchargingapp.ui.evowner.fragments

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.evcharging.evchargingapp.R
import kotlinx.coroutines.CancellationException
import com.evcharging.evchargingapp.databinding.FragmentEvownerProfileBinding
import com.evcharging.evchargingapp.data.repository.UserRepository
import com.evcharging.evchargingapp.data.model.EVOwner
import com.evcharging.evchargingapp.data.model.EVOwnerUpdateRequest
import com.evcharging.evchargingapp.data.model.EVOwnerChangePasswordRequest
import com.evcharging.evchargingapp.ui.LoginActivity
import com.evcharging.evchargingapp.utils.TokenUtils
import com.evcharging.evchargingapp.utils.LoadingManager
import kotlinx.coroutines.launch

class EVOwnerProfileFragment : Fragment() {

    private var _binding: FragmentEvownerProfileBinding? = null
    private val binding get() = _binding!!
    private val userRepository by lazy { UserRepository(requireContext()) }
    private var currentOwner: EVOwner? = null

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
        setupClickListeners()
        loadProfileData()
    }

    private fun setupClickListeners() {
        binding.buttonUpdateProfile.setOnClickListener {
            updateProfile()
        }
        
        binding.buttonDeactivateAccount.setOnClickListener {
            showDeactivateConfirmation()
        }
        
        binding.buttonChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }
        
        binding.buttonLogout.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun loadProfileData() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                LoadingManager.show(requireContext(), "Loading profile...")
                
                val owner = userRepository.getProfile()
                
                if (!isAdded || _binding == null) return@launch
                
                if (owner != null) {
                    currentOwner = owner
                    updateProfileUI(owner)
                } else {
                    showError("Failed to load profile. Please try again.")
                    setPlaceholderData()
                }
            } catch (e: Exception) {
                if (isAdded && _binding != null) {
                    when (e) {
                        is CancellationException -> {
                        }
                        is java.net.ConnectException -> {
                            showError("Connection failed. Check your network.")
                        }
                        is java.net.SocketTimeoutException -> {
                            showError("Request timeout. Please try again.")
                        }
                        else -> {
                            showError("Network error: ${e.message}")
                        }
                    }
                    setPlaceholderData()
                }
            } finally {
                if (isAdded && _binding != null) {
                    LoadingManager.dismiss()
                    showLoading(false)
                }
            }
        }
    }

    private fun updateProfileUI(owner: EVOwner) {
        binding.apply {
            editTextNic.setText(owner.nic)
            editTextName.setText(owner.FullName)
            textViewMemberSince.text = "Member since: ${formatDate(owner.actualCreatedAt)}"
        }
    }

    private fun setPlaceholderData() {
        val userNic = TokenUtils.getCurrentUserNic(requireContext())
        binding.apply {
            editTextNic.setText(userNic ?: "Unknown")
            editTextName.setText("EV Owner")
            textViewMemberSince.text = "Member since: Unknown"
        }
    }

    private fun formatDate(dateString: String): String {
        return try {
            dateString.substringBefore("T")
        } catch (e: Exception) {
            dateString
        }
    }

    private fun updateProfile() {
        val name = binding.editTextName.text.toString().trim()
        
        if (name.isEmpty()) {
            showError("Name is required")
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                LoadingManager.show(requireContext(), "Updating profile...")
                
                val request = EVOwnerUpdateRequest(
                    FullName = name
                )
                
                val response = userRepository.updateOwnProfile(request)
                
                if (!isAdded || _binding == null) return@launch
                
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
                    loadProfileData() // Refresh profile data
                } else {
                    when (response.code()) {
                        401 -> showError("Session expired. Please login again.")
                        404 -> showError("Profile not found.")
                        else -> showError("Failed to update profile: ${response.message()}")
                    }
                }
            } catch (e: Exception) {
                if (isAdded && _binding != null) {
                    when (e) {
                        is CancellationException -> {
                        }
                        is java.net.ConnectException -> {
                            showError("Connection failed. Check your network.")
                        }
                        is java.net.SocketTimeoutException -> {
                            showError("Request timeout. Please try again.")
                        }
                        else -> {
                            showError("Network error: ${e.message}")
                        }
                    }
                }
            } finally {
                if (isAdded && _binding != null) {
                    LoadingManager.dismiss()
                    showLoading(false)
                }
            }
        }
    }

    private fun showChangePasswordDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_change_password, null)
        val currentPasswordInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editTextCurrentPassword)
        val newPasswordInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editTextNewPassword)
        val confirmPasswordInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editTextConfirmPassword)
        val changePasswordButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.buttonChangePassword)
        val cancelButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.buttonCancel)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        // Remove white background from dialog
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Set up button click listeners
        changePasswordButton.setOnClickListener {
            changePassword(
                currentPasswordInput.text.toString(),
                newPasswordInput.text.toString(),
                confirmPasswordInput.text.toString()
            )
            dialog.dismiss()
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun changePassword(currentPassword: String, newPassword: String, confirmPassword: String) {
        if (currentPassword.isEmpty()) {
            showError("Current password is required")
            return
        }
        
        if (newPassword.isEmpty()) {
            showError("New password is required")
            return
        }
        
        if (newPassword.length < 6) {
            showError("Password must be at least 6 characters long")
            return
        }
        
        if (newPassword != confirmPassword) {
            showError("Passwords do not match")
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                LoadingManager.show(requireContext(), "Changing password...")
                
                val request = EVOwnerChangePasswordRequest(
                    CurrentPassword = currentPassword,
                    NewPassword = newPassword
                )
                
                val response = userRepository.changePassword(request)
                
                if (!isAdded || _binding == null) return@launch
                
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Password changed successfully", Toast.LENGTH_SHORT).show()
                } else {
                    when (response.code()) {
                        400 -> showError("Current password is incorrect")
                        401 -> showError("Session expired. Please login again.")
                        else -> showError("Failed to change password: ${response.message()}")
                    }
                }
            } catch (e: Exception) {
                if (isAdded && _binding != null) {
                    when (e) {
                        is CancellationException -> {
                        }
                        is java.net.ConnectException -> {
                            showError("Connection failed. Check your network.")
                        }
                        is java.net.SocketTimeoutException -> {
                            showError("Request timeout. Please try again.")
                        }
                        else -> {
                            showError("Network error: ${e.message}")
                        }
                    }
                }
            } finally {
                if (isAdded && _binding != null) {
                    LoadingManager.dismiss()
                    showLoading(false)
                }
            }
        }
    }

    private fun showDeactivateConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Deactivate Account")
            .setMessage("Are you sure you want to deactivate your account? This action cannot be undone and you will lose access to all your stations and bookings.")
            .setPositiveButton("Deactivate") { _, _ ->
                deactivateAccount()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deactivateAccount() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = userRepository.deactivateOwnAccount()
                
                if (!isAdded || _binding == null) return@launch
                
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Account deactivated successfully", Toast.LENGTH_SHORT).show()
                    
                    // Clear saved token and navigate to login
                    TokenUtils.clearToken(requireContext())
                    
                    val intent = Intent(requireContext(), LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    requireActivity().finish()
                } else {
                    when (response.code()) {
                        401 -> showError("Session expired. Please login again.")
                        404 -> showError("Account not found.")
                        else -> showError("Failed to deactivate account: ${response.message()}")
                    }
                }
            } catch (e: Exception) {
                if (isAdded && _binding != null) {
                    when (e) {
                        is CancellationException -> {
                        }
                        is java.net.ConnectException -> {
                            showError("Connection failed. Check your network.")
                        }
                        is java.net.SocketTimeoutException -> {
                            showError("Request timeout. Please try again.")
                        }
                        else -> {
                            showError("Network error: ${e.message}")
                        }
                    }
                }
            }
        }
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout from your account?")
            .setPositiveButton("Logout") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performLogout() {
        try {
            TokenUtils.clearToken(requireContext())
            
            Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
            
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        } catch (e: Exception) {
            Log.e("EVOwnerProfileFragment", "Error during logout", e)
            showError("Error during logout: ${e.message}")
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.apply {
            buttonUpdateProfile.isEnabled = !isLoading
            buttonChangePassword.isEnabled = !isLoading
            buttonDeactivateAccount.isEnabled = !isLoading
            buttonLogout.isEnabled = !isLoading
            editTextName.isEnabled = !isLoading
            editTextNic.isEnabled = false
        }
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = EVOwnerProfileFragment()
    }
}