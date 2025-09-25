// Author: Peiris M. H. C. (IT22194930)
// Purpose: Station logic, check active bookings before deactivation
using EVChargingAPI.Models;
using EVChargingAPI.Repositories;

namespace EVChargingAPI.Services
{
    public class StationService
    {
        private readonly StationRepository _repo;
        

        public async Task CreateAsync(Station s) => await _repo.CreateAsync(s);
        public async Task<Station?> GetByIdAsync(string id) => await _repo.GetByIdAsync(id);
        public async Task<List<Station>> GetAllAsync() => await _repo.GetAllAsync();

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

    }
}
