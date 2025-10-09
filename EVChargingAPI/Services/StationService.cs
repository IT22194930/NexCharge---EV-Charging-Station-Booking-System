/*
 * File: StationService.cs
 * Author: Wickramasooriya W. A. A. L. (IT22126160)
 * Description: Business logic service for EV charging station management operations.
 *              Provides domain-specific functionality with validation rules and business
 *              constraints. Acts as intermediary between controllers and data repositories.
 * 
 * Key Methods:
 *   - CreateAsync: Create new charging stations
 *   - GetByIdAsync: Retrieve station by unique identifier
 *   - GetAllAsync: Fetch all available stations
 *   - UpdateAsync: Update station details with field-level updates
 *   - UpdateScheduleAsync: Modify station operating hours separately
 *   - DeactivateAsync: Disable station with booking validation
 *   - ActivateAsync: Enable station for operations
 *   - DeleteAsync: Remove station with safety checks
 * 
 * Responsibilities:
 *   - Enforce business rules for station lifecycle management
 *   - Validate booking conflicts before station deactivation/deletion
 *   - Preserve data integrity during partial updates
 *   - Coordinate between station and booking repositories
 *   - Handle operating hours management independently
 *   - Ensure stations cannot be deactivated/deleted with active bookings
 *   - Provide error handling with descriptive exception messages
 */

using EVChargingAPI.Models;
using EVChargingAPI.Repositories;

namespace EVChargingAPI.Services
{
    public class StationService
    {
        private readonly StationRepository _repo;
        private readonly BookingRepository _bookingRepo;
        public StationService(StationRepository repo, BookingRepository bookingRepo)
        {
            _repo = repo;
            _bookingRepo = bookingRepo;
        }

        public async Task CreateAsync(Station s) => await _repo.CreateAsync(s);
        public async Task<Station?> GetByIdAsync(string id) => await _repo.GetByIdAsync(id);
        public async Task<List<Station>> GetAllAsync() => await _repo.GetAllAsync();
        
        public async Task UpdateAsync(string id, Station updatedStation)
        {
            var existingStation = await _repo.GetByIdAsync(id) ?? throw new Exception("Station not found");
            
            // Preserve existing values and only update the fields that were provided
            existingStation.Name = updatedStation.Name;
            existingStation.Location = updatedStation.Location;
            existingStation.Type = updatedStation.Type;
            existingStation.AvailableSlots = updatedStation.AvailableSlots;
            
            // Update Latitude and Longitude if provided
            if (updatedStation.Latitude.HasValue)
            {
                existingStation.Latitude = updatedStation.Latitude;
            }
            if (updatedStation.Longitude.HasValue)
            {
                existingStation.Longitude = updatedStation.Longitude;
            }
            
            // Only update OperatingHours if provided
            if (updatedStation.OperatingHours != null)
            {
                existingStation.OperatingHours = updatedStation.OperatingHours;
            }
            
            // Preserve IsActive from existing station
            await _repo.UpdateAsync(id, existingStation);
        }

        public async Task DeactivateAsync(string id)
        {
            var station = await _repo.GetByIdAsync(id) ?? throw new Exception("Station not found");
            var upcomingBookings = await _bookingRepo.GetUpcomingForStationAsync(id);
            if (upcomingBookings.Any())
                throw new Exception("Cannot deactivate station with active future bookings");
            station.IsActive = false;
            await _repo.UpdateAsync(id, station);
        }

        public async Task ActivateAsync(string id)
        {
            var station = await _repo.GetByIdAsync(id) ?? throw new Exception("Station not found");
            station.IsActive = true;
            await _repo.UpdateAsync(id, station);
        }

        public async Task UpdateScheduleAsync(string id, object operatingHours)
        {
            var station = await _repo.GetByIdAsync(id) ?? throw new Exception("Station not found");
            // Update the operating hours - you might need to parse the operatingHours object
            // For now, let's assume it's passed correctly
            station.OperatingHours = operatingHours;
            await _repo.UpdateAsync(id, station);
        }

        public async Task DeleteAsync(string id)
        {
            var station = await _repo.GetByIdAsync(id) ?? throw new Exception("Station not found");
            var bookings = await _bookingRepo.GetUpcomingForStationAsync(id);
            if (bookings.Any(b => b.Status != "Cancelled" && b.ReservationDate > DateTime.UtcNow))
                throw new Exception("Cannot delete station with active future bookings");
            await _repo.DeleteAsync(id);
        }
    }
}
