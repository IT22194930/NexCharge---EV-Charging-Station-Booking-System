// Author: Peiris M. H. C. (IT22194930)
// Purpose: Dashboard statistics endpoints
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Authorization;
using EVChargingAPI.Repositories;
using System.Security.Claims;

namespace EVChargingAPI.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    [Authorize]
    public class DashboardController : ControllerBase
    {
        private readonly UserRepository _userRepo;
        private readonly StationRepository _stationRepo;
        private readonly BookingRepository _bookingRepo;

        public DashboardController(
            UserRepository userRepo,
            StationRepository stationRepo,
            BookingRepository bookingRepo)
        {
            _userRepo = userRepo;
            _stationRepo = stationRepo;
            _bookingRepo = bookingRepo;
        }

        [HttpGet("stats")]
        public async Task<IActionResult> GetDashboardStats()
        {
            var role = User.FindFirst(ClaimTypes.Role)?.Value;
            var userNic = User.FindFirst(ClaimTypes.Name)?.Value;

            if (role == "EVOwner")
            {
                // For EV Owners, return their personal stats
                if (string.IsNullOrEmpty(userNic))
                {
                    return BadRequest("User identification not found");
                }
                
                var userBookings = await _bookingRepo.GetByOwnerAsync(userNic);
                var activeBookings = userBookings.Where(b => b.Status == "Approved" || b.Status == "Pending").Count();

                return Ok(new
                {
                    totalUsers = 0,
                    totalStations = 0,
                    totalBookings = userBookings.Count(),
                    activeBookings = activeBookings
                });
            }
            else
            {
                // For Backoffice and Operators, return system-wide stats
                var allUsers = await _userRepo.GetAllAsync();
                var allStations = await _stationRepo.GetAllAsync();
                var allBookings = await _bookingRepo.GetAllAsync();
                var activeBookings = allBookings.Where(b => b.Status == "Approved" || b.Status == "Pending").Count();

                return Ok(new
                {
                    totalUsers = allUsers.Count(),
                    totalStations = allStations.Count(),
                    totalBookings = allBookings.Count(),
                    activeBookings = activeBookings
                });
            }
        }
    }
}