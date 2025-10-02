// Author: Welikanna S. T. (IT22196910)
// Purpose: Booking endpoints for owners and operators
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
        public BookingsController(BookingService service, BookingRepository repo) { _service = service; _repo = repo; }

        [HttpPost]
        [Authorize(Roles = "EVOwner")]
        public async Task<IActionResult> Create([FromBody] BookingCreateDto dto)
        {
            try
            {
                var b = new Booking
                {
                    OwnerNIC = dto.OwnerNic,
                    StationId = dto.StationId,
                    ReservationDate = dto.ReservationDate,
                    ReservationHour = dto.ReservationHour
                };
                var created = await _service.CreateAsync(b);
                return Ok(created);
            }
            catch (Exception ex) { return BadRequest(ex.Message); }
        }

        [HttpPut("{id}")]
        [Authorize(Roles = "EVOwner")]
        public async Task<IActionResult> Update(string id, [FromBody] BookingUpdateDto dto)
        {
            try
            {
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
                var b = await _service.ApproveAsync(id);
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
                await _service.DeleteAsync(id);
                return Ok();
            }
            catch (Exception ex) { return BadRequest(ex.Message); }
        }

        [HttpGet("owner/{ownerNic}")]
        [Authorize(Roles = "EVOwner,Operator,Backoffice")]
        public async Task<IActionResult> GetByOwner(string ownerNic)
        {
            var list = await _repo.GetByOwnerAsync(ownerNic);
            return Ok(list);
        }

        [HttpGet]
        [Authorize(Roles = "Operator,Backoffice")]
        public async Task<IActionResult> GetAll()
        {
            var list = await _repo.GetAllAsync();
            return Ok(list);
        }

        [HttpGet("availability/{stationId}")]
        [Authorize(Roles = "EVOwner,Operator,Backoffice")]
        public async Task<IActionResult> GetStationAvailability(string stationId, [FromQuery] DateTime date)
        {
            try
            {
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
                var availableHours = await _service.GetAvailableHoursAsync(stationId, date);
                return Ok(availableHours);
            }
            catch (Exception ex) { return BadRequest(ex.Message); }
        }
    }
}
