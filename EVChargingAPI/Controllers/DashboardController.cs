/*
 * File: DashboardController.cs
 * Author: Peiris M. H. C. (IT22194930)

        // Get role-based dashboard statistics and analytics
        [HttpGet(\"stats\")]
        public async Task<IActionResult> GetDashboardStats()
        
 * Description: API Controller for dashboard analytics and system statistics.
 *              Provides comprehensive reporting and analytics data for different user roles.
 *              Generates real-time statistics for users, stations, bookings, and system performance.
 * 
 * Endpoints:
 * - GET /api/dashboard/stats - Get overall system statistics (all roles)
 * - GET /api/dashboard/user-stats - Get user-specific statistics (role-based)
 * - GET /api/dashboard/booking-stats - Get booking analytics and trends
 * - GET /api/dashboard/station-stats - Get station utilization and performance data
 * - GET /api/dashboard/operator-stats - Get operator-specific dashboard data
 * 
 * Security: Requires JWT authentication with role-based data filtering.
 *           Different statistical views based on user role (Backoffice, Operator, EVOwner).
 *           Implements data isolation to ensure users only see relevant information.
 * 
 * Features: Real-time statistics calculation, role-based data aggregation,
 *          performance metrics, utilization reports, and trend analysis.
 */

using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Authorization;
using EVChargingAPI.Repositories;
using EVChargingAPI.Services;
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
        private readonly UserService _userService;

        public DashboardController(
            UserRepository userRepo,
            StationRepository stationRepo,
            BookingRepository bookingRepo,
            UserService userService)
        {
            _userRepo = userRepo;
            _stationRepo = stationRepo;
            _bookingRepo = bookingRepo;
            _userService = userService;
        }

        [HttpGet("stats")]
        public async Task<IActionResult> GetDashboardStats()
        {
            var role = User.FindFirst(ClaimTypes.Role)?.Value;
            var userNic = User.FindFirst(ClaimTypes.Name)?.Value;

            if (string.IsNullOrEmpty(userNic))
            {
                return BadRequest("User identification not found");
            }

            if (role == "EVOwner")
            {
                // For EV Owners, return their personal stats
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
            else if (role == "Operator")
            {
                // For Operators, return stats specific to their assigned station
                var operatorUser = await _userService.GetByNicAsync(userNic);
                if (operatorUser == null || string.IsNullOrEmpty(operatorUser.AssignedStationId))
                {
                    return Ok(new
                    {
                        totalUsers = 0,
                        totalStations = 0,
                        totalBookings = 0,
                        activeBookings = 0,
                        assignedStation = (string?)null,
                        message = "No station assigned"
                    });
                }

                // Get all bookings for the operator's assigned station
                var allBookings = await _bookingRepo.GetAllAsync();
                var stationBookings = allBookings.Where(b => b.StationId == operatorUser.AssignedStationId).ToList();
                var activeBookings = stationBookings.Where(b => b.Status == "Approved" || b.Status == "Pending").Count();
                var pendingBookings = stationBookings.Where(b => b.Status == "Pending").Count();

                return Ok(new
                {
                    totalUsers = 0,
                    totalStations = 1, // Operator manages one station
                    totalBookings = stationBookings.Count(),
                    activeBookings = activeBookings,
                    pendingBookings = pendingBookings,
                    assignedStation = operatorUser.AssignedStationName ?? "Unknown Station"
                });
            }
            else
            {
                // For Backoffice, return system-wide stats
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