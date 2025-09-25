// Author: Peiris M. H. C. (IT22194930)
// Purpose: User management endpoints (Backoffice)
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Authorization;
using EVChargingAPI.Services;
using EVChargingAPI.DTOs;
using EVChargingAPI.Models;

namespace EVChargingAPI.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    [Authorize(Roles = "Backoffice")]
    public class UsersController : ControllerBase
    {
        private readonly UserService _service;
        public UsersController(UserService service) { _service = service; }

        [HttpPost]
        public async Task<IActionResult> Create([FromBody] UserCreateDto dto)
        {
            try
            {
                // Check if user already exists
                var existingUser = await _service.GetByNicAsync(dto.Nic);
                if (existingUser != null)
                {
                    return BadRequest("User with this NIC already exists");
                }

                // Validate role
                if (dto.Role != "Backoffice" && dto.Role != "Operator" && dto.Role != "EVOwner")
                {
                    return BadRequest("Invalid role. Must be 'Backoffice', 'Operator', or 'EVOwner'");
                }

                // Create new user
                var user = new User
                {
                    NIC = dto.Nic,
                    FullName = dto.FullName,
                    Role = dto.Role,
                    PasswordHash = BCrypt.Net.BCrypt.HashPassword(dto.Password),
                    IsActive = true,
                    CreatedAt = DateTime.UtcNow
                };

                await _service.CreateAsync(user);
            }
            catch (Exception ex)
            {
                return BadRequest($"Error creating user: {ex.Message}");
            }
        }

     
    }
}
