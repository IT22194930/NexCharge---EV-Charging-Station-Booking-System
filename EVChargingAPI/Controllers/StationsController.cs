/*
 * File: StationsController.cs
 * Author: Wickramasooriya W. A. A. L. (IT22126160)
 * Description: Controller for managing EV charging stations with full CRUD operations.
 *              Handles station creation, updates, schedule management, and status control
 *              for both backoffice administrators and operators.
 * 
 * Endpoints:
 *   GET    /api/stations           - Retrieve all stations (Public)
 *   GET    /api/stations/{id}      - Retrieve station by ID (Public)
 *   POST   /api/stations           - Create new station (Backoffice only)
 *   PUT    /api/stations/{id}      - Update station details (Backoffice only)
 *   PUT    /api/stations/{id}/schedule - Update station operating hours (Backoffice only)
 *   DELETE /api/stations/{id}      - Delete station (Backoffice only)
 *   POST   /api/stations/deactivate/{id} - Deactivate station (Backoffice only)
 *   POST   /api/stations/activate/{id}   - Activate station (Backoffice only)
 * 
 * Security:
 *   - JWT Bearer token authentication required for all operations except GET endpoints
 *   - Role-based authorization: "Backoffice" role required for CUD operations
 *   - Public read access for station information retrieval
 *   - Fine-grained permissions per endpoint method
 * 
 * Business Rules:
 *   - Only backoffice users can create, update, or delete stations
 *   - Station information is publicly accessible for customer viewing
 *   - Stations can be activated/deactivated without deletion for operational control
 *   - Operating hours can be updated separately from other station details
 *   - All stations must have required fields: Name, Location, Type, AvailableSlots
 *   - Coordinates (Latitude/Longitude) are optional for mapping functionality
 *   - New stations are created as active by default
 */

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

        [HttpGet("{id}")]
        [AllowAnonymous]
        public async Task<IActionResult> GetById(string id)
        {
            var station = await _service.GetByIdAsync(id);
            if (station == null) return NotFound();
            return Ok(station);
        }

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

        [HttpPut("{id}")]
        [Authorize(Roles = "Backoffice")]
        public async Task<IActionResult> Update(string id, [FromBody] dynamic requestData)
        {
            try
            {
                Console.WriteLine($"Updating station with ID: {id}");
                
                // Convert to JSON string and back to JsonElement for processing
                var jsonString = JsonSerializer.Serialize(requestData);
                var jsonDocument = JsonDocument.Parse(jsonString);
                var root = jsonDocument.RootElement;
                
                Console.WriteLine($"Parsed JSON: {jsonString}");
                
                // Create a Station object from the dynamic data with simple object types
                var station = new Station
                {
                    Id = id,
                    Name = root.TryGetProperty("name", out JsonElement nameElement) ? nameElement.GetString() : null,
                    Location = root.TryGetProperty("location", out JsonElement locationElement) ? locationElement.GetString() : null,
                    Type = root.TryGetProperty("type", out JsonElement typeElement) ? typeElement.GetString() : null,
                    AvailableSlots = root.TryGetProperty("availableSlots", out JsonElement slotsElement) ? slotsElement.GetInt32() : 0,
                    Latitude = root.TryGetProperty("lat", out JsonElement latElement) ? (latElement.ValueKind == JsonValueKind.Null ? null : latElement.GetDouble()) : null,
                    Longitude = root.TryGetProperty("lng", out JsonElement lngElement) ? (lngElement.ValueKind == JsonValueKind.Null ? null : lngElement.GetDouble()) : null,
                    OperatingHours = root.TryGetProperty("operatingHours", out JsonElement hoursElement) ? 
                        new {
                            openTime = hoursElement.TryGetProperty("openTime", out JsonElement openTimeElement) ? openTimeElement.GetString() : "06:00",
                            closeTime = hoursElement.TryGetProperty("closeTime", out JsonElement closeTimeElement) ? closeTimeElement.GetString() : "22:00",
                            isOpen24Hours = hoursElement.TryGetProperty("isOpen24Hours", out JsonElement is24HoursElement) ? is24HoursElement.GetBoolean() : false
                        } : null
                };
                
                Console.WriteLine($"Station object: Name={station.Name}, Location={station.Location}, Type={station.Type}, AvailableSlots={station.AvailableSlots}, Latitude={station.Latitude}, Longitude={station.Longitude}, OperatingHours={station.OperatingHours}");
                
                await _service.UpdateAsync(id, station);
                
                return Ok(new { message = "Station updated successfully", station });
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Update error: {ex.Message}");
                Console.WriteLine($"Stack trace: {ex.StackTrace}");
                return BadRequest(new { message = "Failed to update station", error = ex.Message });
            }
        }

        [HttpPut("{id}/schedule")]
        [Authorize(Roles = "Backoffice")]
        public async Task<IActionResult> UpdateSchedule(string id, [FromBody] dynamic operatingHours)
        {
            try
            {
                Console.WriteLine($"Updating schedule for station ID: {id}");
                Console.WriteLine($"Operating hours data: {operatingHours}");
                
                // Convert to JSON string and back to JsonElement for processing
                var jsonString = JsonSerializer.Serialize(operatingHours);
                var jsonDocument = JsonDocument.Parse(jsonString);
                var root = jsonDocument.RootElement;
                
                Console.WriteLine($"Parsed JSON: {jsonString}");
                
                // Create a properly serializable operating hours object
                var hoursObj = new {
                    openTime = root.TryGetProperty("openTime", out JsonElement openTimeElement) ? openTimeElement.GetString() : "06:00",
                    closeTime = root.TryGetProperty("closeTime", out JsonElement closeTimeElement) ? closeTimeElement.GetString() : "22:00",
                    isOpen24Hours = root.TryGetProperty("isOpen24Hours", out JsonElement is24HoursElement) ? is24HoursElement.GetBoolean() : false
                };
                
                await _service.UpdateScheduleAsync(id, hoursObj);
                return Ok(new { message = "Station schedule updated successfully" });
            }
            catch (Exception ex) 
            { 
                Console.WriteLine($"Schedule update error: {ex.Message}");
                Console.WriteLine($"Stack trace: {ex.StackTrace}");
                return BadRequest(new { message = "Failed to update station schedule", error = ex.Message }); 
            }
        }

        [HttpDelete("{id}")]
        [Authorize(Roles = "Backoffice")]
        public async Task<IActionResult> Delete(string id)
        {
            try
            {
                await _service.DeleteAsync(id);
                return Ok();
            }
            catch (Exception ex) { return BadRequest(ex.Message); }
        }

        [HttpPost("deactivate/{id}")]
        [Authorize(Roles = "Backoffice")]
        public async Task<IActionResult> Deactivate(string id)
        {
            try
            {
                await _service.DeactivateAsync(id);
                return Ok();
            }
            catch (Exception ex) { return BadRequest(ex.Message); }
        }

        [HttpPost("activate/{id}")]
        [Authorize(Roles = "Backoffice")]
        public async Task<IActionResult> Activate(string id)
        {
            try
            {
                await _service.ActivateAsync(id);
                return Ok();
            }
            catch (Exception ex) { return BadRequest(ex.Message); }
        }
    }
}
