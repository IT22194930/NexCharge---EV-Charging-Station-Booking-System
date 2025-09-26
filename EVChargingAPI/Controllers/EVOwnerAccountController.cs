/*
 * File: EVOwnerAccountController.cs
 * Author: Liyanage N S
 * Date: September 26, 2025
 * Description: API Controller for EV Owner account management operations.
 *              Handles registration, profile management, password changes, and account deactivation.
 *              Provides secure endpoints with JWT authentication and role-based authorization.
 * 
 * Endpoints:
 * - POST /api/evowner/register - Register new EV Owner account
 * - GET /api/evowner/profile - Get authenticated user's profile
 * - PUT /api/evowner/profile - Update authenticated user's profile
 * - PUT /api/evowner/profile/change-password - Change user password
 * - PUT /api/evowner/profile/deactivate - Deactivate user account
 * 
 * Security: Requires JWT authentication for all endpoints except registration.
 *           Uses BCrypt for password hashing and verification.
 */

using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Authorization;
using EVChargingAPI.Services;
using EVChargingAPI.DTOs;
using EVChargingAPI.Models;
using System.Security.Claims;

namespace EVChargingAPI.Controllers
{
    [ApiController]
    [Route("api/evowner")]
    public class EVOwnerAccountController : ControllerBase
    {
        private readonly UserService _userService;
        private readonly AuthService _authService;

        public EVOwnerAccountController(UserService userService, AuthService authService)
        {
            _userService = userService;
            _authService = authService;
        }

        // Public endpoint for EV Owner registration
        [HttpPost("register")]
        public async Task<IActionResult> RegisterEVOwner([FromBody] EVOwnerRegisterDto dto)
        {
            try
            {
                // Check if user already exists
                var existingUser = await _userService.GetByNicAsync(dto.Nic);
                if (existingUser != null)
                {
                    return BadRequest("User with this NIC already exists");
                }

                // Create new EV Owner
                var user = await _authService.RegisterEVOwnerAsync(dto.Nic, dto.FullName, dto.Password);

                // Return user info (without sensitive data)
                return CreatedAtAction(nameof(GetProfile), new { nic = user.NIC }, new
                {
                    user.NIC,
                    user.FullName,
                    user.Role,
                    user.IsActive,
                    user.CreatedAt
                });
            }
            catch (Exception ex)
            {
                return BadRequest($"Error creating account: {ex.Message}");
            }
        }

        // Get own profile (authenticated EV Owner only)
        [HttpGet("profile")]
        [Authorize(Roles = "EVOwner")]
        public async Task<IActionResult> GetProfile()
        {
            try
            {
                var userNic = User.FindFirst(ClaimTypes.Name)?.Value;
                if (string.IsNullOrEmpty(userNic))
                {
                    return Unauthorized("Unable to get user information from token");
                }

                var user = await _userService.GetByNicAsync(userNic);
                if (user == null)
                {
                    return NotFound("User profile not found");
                }

                // Return user info (without sensitive data)
                return Ok(new
                {
                    user.NIC,
                    user.FullName,
                    user.Role,
                    user.IsActive,
                    user.CreatedAt
                });
            }
            catch (Exception ex)
            {
                return BadRequest($"Error retrieving profile: {ex.Message}");
            }
        }

        // Update own profile (authenticated EV Owner only)
        [HttpPut("profile")]
        [Authorize(Roles = "EVOwner")]
        public async Task<IActionResult> UpdateProfile([FromBody] EVOwnerUpdateDto dto)
        {
            try
            {
                var userNic = User.FindFirst("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name")?.Value;
                if (string.IsNullOrEmpty(userNic))
                {
                    return Unauthorized("Unable to get user information from token");
                }

                var existingUser = await _userService.GetByNicAsync(userNic);
                if (existingUser == null)
                {
                    return NotFound("User not found");
                }

                // Update user properties
                existingUser.FullName = dto.FullName ?? existingUser.FullName;

                // Only update password if provided
                if (!string.IsNullOrEmpty(dto.Password))
                {
                    existingUser.PasswordHash = BCrypt.Net.BCrypt.HashPassword(dto.Password);
                }

                await _userService.UpdateAsync(existingUser.Id!, existingUser);

                return Ok(new
                {
                    message = "Profile updated successfully",
                    user = new
                    {
                        existingUser.NIC,
                        existingUser.FullName,
                        existingUser.Role,
                        existingUser.IsActive,
                        existingUser.CreatedAt
                    }
                });
            }
            catch (Exception ex)
            {
                return BadRequest($"Error updating profile: {ex.Message}");
            }
        }

        // Deactivate own account (authenticated EV Owner only)
        [HttpPut("profile/change-password")]
        [Authorize(Roles = "EVOwner")]
        public async Task<IActionResult> ChangePassword([FromBody] EVOwnerChangePasswordDto dto)
        {
            try
            {
                var userNic = User.FindFirst("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name")?.Value;
                if (string.IsNullOrEmpty(userNic))
                {
                    return Unauthorized("Unable to get user information from token");
                }

                var existingUser = await _userService.GetByNicAsync(userNic);
                if (existingUser == null)
                {
                    return NotFound("User not found");
                }

                // Verify current password
                if (!BCrypt.Net.BCrypt.Verify(dto.CurrentPassword, existingUser.PasswordHash))
                {
                    return BadRequest("Current password is incorrect");
                }

                // Update with new password
                existingUser.PasswordHash = BCrypt.Net.BCrypt.HashPassword(dto.NewPassword);
                await _userService.UpdateAsync(existingUser.Id!, existingUser);

                return Ok(new { message = "Password changed successfully" });
            }
            catch (Exception ex)
            {
                return BadRequest($"Error changing password: {ex.Message}");
            }
        }

        [HttpPut("profile/deactivate")]
        [Authorize(Roles = "EVOwner")]
        public async Task<IActionResult> DeactivateAccount()
        {
            try
            {
                var userNic = User.FindFirst("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name")?.Value;
                if (string.IsNullOrEmpty(userNic))
                {
                    return Unauthorized("Unable to get user information from token");
                }

                await _userService.DeactivateByNicAsync(userNic);

                return Ok(new { message = "Account deactivated successfully. Contact back-office to reactivate." });
            }
            catch (Exception ex)
            {
                return BadRequest($"Error deactivating account: {ex.Message}");
            }
        }
    }
}