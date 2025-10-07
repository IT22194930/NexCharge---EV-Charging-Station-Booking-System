/*
 * File: AuthController.cs
 * Author: Peiris M. H. C. (IT22194930)
 * Description: API Controller for user authentication and authorization operations.
 *              Handles user login, registration, and profile retrieval with secure JWT token-based authentication.
 *              Provides comprehensive authentication services for all user roles in the system.
 * 
 * Endpoints:
 * - POST /api/auth/login - Authenticate user credentials and return JWT token
 * - POST /api/auth/register - Register new EV Owner account (public endpoint)
 * - GET /api/auth/profile - Get authenticated user's profile information
 * 
 * Security: Uses JWT Bearer token authentication for protected endpoints.
 *           Implements BCrypt password hashing for secure credential storage.
 *           Supports role-based authorization for Backoffice, Operator, and EVOwner roles.
 * 
 * Dependencies: AuthService for authentication logic, UserService for user data operations.
 */

using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Authorization;
using EVChargingAPI.Services;
using EVChargingAPI.DTOs;
using System.Security.Claims;

namespace EVChargingAPI.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    public class AuthController : ControllerBase
    {
        private readonly AuthService _auth;
        private readonly UserService _userService;
        public AuthController(AuthService auth, UserService userService) 
        { 
            _auth = auth; 
            _userService = userService;
        }

        // Authenticate user credentials and return JWT token
        [HttpPost("login")]
        public async Task<IActionResult> Login([FromBody] LoginRequest req)
        {
            var token = await _auth.AuthenticateAsync(req.Nic, req.Password);
            if (token == null) return Unauthorized("Invalid credentials");
            return Ok(new LoginResponse(token));
        }

        // Register new EV Owner account (public endpoint)
        [HttpPost("register")]
        public async Task<IActionResult> Register([FromBody] RegisterRequest req)
        {
            try
            {
                var user = await _auth.RegisterEVOwnerAsync(req.Nic, req.FullName, req.Password);
                return Ok(new { user.NIC, user.FullName, user.Role });
            }
            catch (Exception ex)
            {
                return BadRequest(ex.Message);
            }
        }

        // Get authenticated user's profile information
        [HttpGet("profile")]
        [Authorize]
        public async Task<IActionResult> GetProfile()
        {
            try
            {
                // Get the current user's NIC from the JWT token
                var userNic = User.FindFirst(ClaimTypes.Name)?.Value;
                if (string.IsNullOrEmpty(userNic))
                {
                    return Unauthorized("Invalid token");
                }

                // Get user data from database
                var user = await _userService.GetByNicAsync(userNic);
                if (user == null)
                {
                    return NotFound("User not found");
                }

                // Return user profile data
                return Ok(new 
                {
                    user.NIC,
                    user.FullName,
                    user.Role,
                    user.AssignedStationId,
                    user.AssignedStationName,
                    user.IsActive
                });
            }
            catch (Exception ex)
            {
                return BadRequest($"Error getting profile: {ex.Message}");
            }
        }
    }
}
