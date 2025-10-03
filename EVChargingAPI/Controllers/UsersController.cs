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

                await _service.CreateWithStationAsync(user, dto.AssignedStationId);
                
                return CreatedAtAction(nameof(GetByNic), new { nic = user.NIC }, user);
            }
            catch (Exception ex)
            {
                return BadRequest($"Error creating user: {ex.Message}");
            }
        }

        [HttpGet]
        public async Task<IActionResult> GetAll() => Ok(await _service.GetAllAsync());

        [HttpGet("{nic}")]
        public async Task<IActionResult> GetByNic(string nic)
        {
            var u = await _service.GetByNicAsync(nic);
            if (u == null) return NotFound();
            return Ok(u);
        }

        [HttpPut("{nic}")]
        public async Task<IActionResult> Update(string nic, [FromBody] UserUpdateDto dto)
        {
            try
            {
                // Find the user by NIC
                var existingUser = await _service.GetByNicAsync(nic);
                if (existingUser == null)
                {
                    return NotFound("User not found");
                }

                // Update the user properties
                existingUser.FullName = dto.FullName ?? existingUser.FullName;
                
                // Only update password if provided
                if (!string.IsNullOrEmpty(dto.Password))
                {
                    existingUser.PasswordHash = BCrypt.Net.BCrypt.HashPassword(dto.Password);
                }

                // Update using the service
                await _service.UpdateAsync(existingUser.Id!, existingUser);
                
                return Ok("User updated successfully");
            }
            catch (Exception ex)
            {
                return BadRequest($"Error updating user: {ex.Message}");
            }
        }

        [HttpPost("deactivate/{nic}")]
        public async Task<IActionResult> Deactivate(string nic)
        {
            try
            {
                await _service.DeactivateByNicAsync(nic);
                return Ok();
            }
            catch (Exception ex) { return BadRequest(ex.Message); }
        }

        [HttpPost("activate/{nic}")]
        public async Task<IActionResult> Activate(string nic)
        {
            try
            {
                await _service.ActivateByNicAsync(nic);
                return Ok();
            }
            catch (Exception ex) { return BadRequest(ex.Message); }
        }

        [HttpPatch("{nic}/role")]
        public async Task<IActionResult> UpdateRole(string nic, [FromBody] UpdateRoleDto dto)
        {
            try
            {
                // Validate role
                if (dto.NewRole != "Backoffice" && dto.NewRole != "Operator" && dto.NewRole != "EVOwner")
                {
                    return BadRequest("Invalid role. Must be 'Backoffice', 'Operator', or 'EVOwner'");
                }

                await _service.UpdateRoleByNicAsync(nic, dto.NewRole, dto.AssignedStationId);
                return Ok();
            }
            catch (Exception ex) { return BadRequest(ex.Message); }
        }

        [HttpPatch("{nic}/station")]
        public async Task<IActionResult> UpdateStationAssignment(string nic, [FromBody] UpdateStationAssignmentDto dto)
        {
            try
            {
                await _service.UpdateStationAssignmentByNicAsync(nic, dto.AssignedStationId);
                return Ok("Station assignment updated successfully");
            }
            catch (Exception ex) { return BadRequest(ex.Message); }
        }

        [HttpDelete("{nic}")]
        public async Task<IActionResult> Delete(string nic)
        {
            try
            {
                await _service.DeleteByNicAsync(nic);
                return Ok();
            }
            catch (Exception ex) { return BadRequest(ex.Message); }
        }
    }
}
