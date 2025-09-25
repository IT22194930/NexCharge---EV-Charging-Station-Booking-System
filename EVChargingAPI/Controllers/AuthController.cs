// Author: Peiris M. H. C. (IT22194930)
// Purpose: Authentication endpoints
using Microsoft.AspNetCore.Mvc;
using EVChargingAPI.Services;
using EVChargingAPI.DTOs;

namespace EVChargingAPI.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    public class AuthController : ControllerBase
    {
        private readonly AuthService _auth;
        public AuthController(AuthService auth) { _auth = auth; }

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
    }
}
