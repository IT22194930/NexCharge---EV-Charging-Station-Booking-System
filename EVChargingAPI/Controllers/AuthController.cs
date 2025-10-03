// Author: Peiris M. H. C. (IT22194930)
// Purpose: Authentication endpoints
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

        [HttpPost("login")]
        public async Task<IActionResult> Login([FromBody] LoginRequest req)
        {
            var token = await _auth.AuthenticateAsync(req.Nic, req.Password);
            if (token == null) return Unauthorized("Invalid credentials");
            return Ok(new LoginResponse(token));
        }

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
