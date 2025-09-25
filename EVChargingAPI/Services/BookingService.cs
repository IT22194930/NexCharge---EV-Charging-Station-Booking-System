// Author: Welikanna S. T. (IT22196910)
// Purpose: Booking logic enforcing rules
using EVChargingAPI.Models;
using EVChargingAPI.Repositories;

namespace EVChargingAPI.Services
{
    public class BookingService
    {
        private readonly BookingRepository _repo;
        private readonly StationRepository _stations;
        private readonly UserRepository _users;
        

        public BookingService(BookingRepository repo, StationRepository stations, UserRepository users)
        {
            _repo = repo;
            _stations = stations;
            _users = users;
            
        }

        public async Task<Booking> CreateAsync(Booking b)
        {
            // Validate owner exists and active
            var owner = await _users.GetByNICAsync(b.OwnerNIC) ?? throw new Exception("Owner not found");
            if (!owner.IsActive) throw new Exception("Owner not active");

            // Validate station exists and active
            var station = await _stations.GetByIdAsync(b.StationId) ?? throw new Exception("Station not found");
            if (!station.IsActive) throw new Exception("Station not active");

            // Booking date must be within 7 days from now
            if (b.ReservationDate > DateTime.UtcNow.AddDays(7) || b.ReservationDate < DateTime.UtcNow)
                throw new Exception("Reservation must be within 7 days and in the future");

            // Create booking as Pending
            b.Status = "Pending";
            await _repo.CreateAsync(b);
            return b;
        }

        

        

        
    }
}
