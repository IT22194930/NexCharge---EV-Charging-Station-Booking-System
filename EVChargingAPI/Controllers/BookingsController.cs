/*
 * File: BookingsController.cs
 * Author: Welikanna S. T. (IT22196910)
 * Description: API Controller for booking management operations.
 *              Exposes endpoints to create, update, approve, complete, cancel, retrieve, and delete bookings
 *              with validation and role-based authorization.
 * 
 * Endpoints:
 * - GET    /api/bookings                  - Get all bookings (role-scoped)
 * - GET    /api/bookings/{id}             - Get a booking by ID
 * - POST   /api/bookings                  - Create a new booking
 * - PUT    /api/bookings/{id}             - Update an existing booking
 * - POST   /api/bookings/approve/{id}     - Approve a pending booking (generates QR)
 * - POST   /api/bookings/complete/{id}    - Complete an approved booking
 * - POST   /api/bookings/cancel/{id}      - Cancel a pending booking
 * - DELETE /api/bookings/{id}             - Delete a booking (guarded by workflow rules)
 * 
 * Security: Requires JWT authentication. Role-based authorization (EVOwner, Operator, Backoffice).
 *           Delegates business rule enforcement to BookingService and returns appropriate HTTP codes.
 */

using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Authorization;
using EVChargingAPI.Services;
using EVChargingAPI.DTOs;
using EVChargingAPI.Models;

using EVChargingAPI.Repositories;

namespace EVChargingAPI.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    [Authorize]
    public class BookingsController : ControllerBase
    {
        private readonly BookingService _service;
        private readonly BookingRepository _repo; // for simple retrieval

        // DI-injected controller: service encapsulates business rules; repo used for read-only lookups.
        public BookingsController(BookingService service, BookingRepository repo) { _service = service; _repo = repo; }

        // NEW: Get single booking by id (needed for operator QR scan verification)
        [HttpGet("{id}")]
        [Authorize(Roles = "EVOwner,Operator,Backoffice")]
        public async Task<IActionResult> GetById(string id)
        {
            var b = await _repo.GetByIdAsync(id);
            if (b == null) return NotFound(); // 404 when the booking cannot be found
            return Ok(b); // 200 with booking body
        }

        [HttpPost]
        [Authorize(Roles = "EVOwner")]
        public async Task<IActionResult> Create([FromBody] BookingCreateDto dto)
        {
            try
            {
                // Map DTO to domain model; status set by service as "Pending"
                var b = new Booking
                {
                    OwnerNIC = dto.OwnerNic,
                    StationId = dto.StationId,
                    ReservationDate = dto.ReservationDate,
                    ReservationHour = dto.ReservationHour
                };
                var created = await _service.CreateAsync(b);
                return Ok(created); // 200 OK; could be 201 Created in future with Location header
            }
            catch (Exception ex) { return BadRequest(ex.Message); } // 400 with error text
        }

        [HttpPut("{id}")]
        [Authorize(Roles = "EVOwner")]
        public async Task<IActionResult> Update(string id, [FromBody] BookingUpdateDto dto)
        {
            try
            {
                // Only reservation fields are updatable; service enforces 12-hour rule and availability
                var updated = await _service.UpdateAsync(id, new Booking { ReservationDate = dto.ReservationDate, ReservationHour = dto.ReservationHour, StationId = dto.StationId });
                return Ok(updated);
            }
            catch (Exception ex) { return BadRequest(ex.Message); }
        }

        [HttpPost("cancel/{id}")]
        [Authorize(Roles = "EVOwner,Operator,Backoffice")]
        public async Task<IActionResult> Cancel(string id)
        {
            try
            {
                // Cancels a booking if time-window allows; status becomes "Cancelled"
                await _service.CancelAsync(id);
                return Ok();
            }
            catch (Exception ex) { return BadRequest(ex.Message); }
        }

        [HttpPost("approve/{id}")]
        [Authorize(Roles = "Operator,Backoffice")]
        public async Task<IActionResult> Approve(string id)
        {
            try
            {
                // Approves a Pending booking; QR image payload generated and persisted
                var b = await _service.ApproveAsync(id);
                return Ok(b);
            }
            catch (Exception ex) { return BadRequest(ex.Message); }
        }

        [HttpPost("confirm/{id}")]
        [Authorize(Roles = "Operator,Backoffice")]
        public async Task<IActionResult> Confirm(string id)
        {
            try
            {
                // Moves from Approved → Started (arrival/scan step)
                var b = await _service.ConfirmAsync(id);
                return Ok(b);
            }
            catch (Exception ex) { return BadRequest(ex.Message); }
        }

        [HttpPost("complete/{id}")]
        [Authorize(Roles = "Operator,Backoffice")]
        public async Task<IActionResult> Complete(string id)
        {
            try
            {
                // Completes a session from Started (or directly from Approved as a safe fallback)
                var b = await _service.CompleteAsync(id);
                return Ok(b);
            }
            catch (Exception ex) { return BadRequest(ex.Message); }
        }

        [HttpDelete("{id}")]
        [Authorize(Roles = "EVOwner,Backoffice")]
        public async Task<IActionResult> Delete(string id)
        {
            try
            {
                // Deletes Pending/Cancelled bookings; Approved/Completed are protected by service guard
                await _service.DeleteAsync(id);
                return Ok();
            }
            catch (Exception ex) { return BadRequest(ex.Message); }
        }

        [HttpGet("owner/{ownerNic}")]
        [Authorize(Roles = "EVOwner,Operator,Backoffice")]
        public async Task<IActionResult> GetByOwner(string ownerNic)
        {
            // Simple owner-scoped listing; callers should apply additional filters on the client if needed
            var list = await _repo.GetByOwnerAsync(ownerNic);
            return Ok(list);
        }

        [HttpGet]
        [Authorize(Roles = "Operator,Backoffice")]
        public async Task<IActionResult> GetAll()
        {
            // Unscoped list for operational dashboards (role-restricted)
            var list = await _repo.GetAllAsync();
            return Ok(list);
        }

        [HttpGet("availability/{stationId}")]
        [Authorize(Roles = "EVOwner,Operator,Backoffice")]
        public async Task<IActionResult> GetStationAvailability(string stationId, [FromQuery] DateTime date)
        {
            try
            {
                // Returns per-hour available slot counts for the requested station/date
                var availability = await _service.GetStationAvailabilityAsync(stationId, date);
                return Ok(availability);
            }
            catch (Exception ex) { return BadRequest(ex.Message); }
        }

        [HttpGet("available-hours/{stationId}")]
        [Authorize(Roles = "EVOwner,Operator,Backoffice")]
        public async Task<IActionResult> GetAvailableHours(string stationId, [FromQuery] DateTime date)
        {
            try
            {
                // Returns list of hour indexes [0..23] that are not fully booked
                var availableHours = await _service.GetAvailableHoursAsync(stationId, date);
                return Ok(availableHours);
            }
            catch (Exception ex) { return BadRequest(ex.Message); }
        }
    }
}
