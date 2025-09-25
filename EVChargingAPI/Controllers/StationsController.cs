// Author: Wickramasooriya W. A. A. L. (IT22126160)
// Purpose: Station management (Backoffice + Operator)
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Authorization;
using EVChargingAPI.Services;
using EVChargingAPI.DTOs;
using EVChargingAPI.Models;
using System.Text.Json;

namespace EVChargingAPI.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    [Authorize] // both Backoffice and Operator can access; fine-grain via roles per method
    public class StationsController : ControllerBase
    {
        private readonly StationService _service;
        public StationsController(StationService service) { _service = service; }

        [HttpGet]
        [AllowAnonymous]
        public async Task<IActionResult> GetAll() => Ok(await _service.GetAllAsync());

        [HttpPost]
        [Authorize(Roles = "Backoffice")]
        public async Task<IActionResult> Create([FromBody] StationCreateDto dto)
        {
            var s = new Station
            {
                Name = dto.Name,
                Location = dto.Location,
                Latitude = dto.Lat,
                Longitude = dto.Lng,
                Type = dto.Type,
                AvailableSlots = dto.AvailableSlots,
                IsActive = true
            };
            await _service.CreateAsync(s);
            return Ok(s);
        }
    }
}
