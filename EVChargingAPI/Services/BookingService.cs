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
        private readonly QrCodeService _qr;

        public BookingService(BookingRepository repo, StationRepository stations, UserRepository users, QrCodeService qr)
        {
            _repo = repo;
            _stations = stations;
            _users = users;
            _qr = qr;
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

        public async Task<Booking> UpdateAsync(string id, Booking updated)
        {
            var existing = await _repo.GetByIdAsync(id) ?? throw new Exception("Booking not found");
            // Only allow update >=12 hours before reservation
            if (DateTime.UtcNow > existing.ReservationDate.AddHours(-12))
                throw new Exception("Cannot modify booking less than 12 hours before reservation");

            existing.ReservationDate = updated.ReservationDate;
            existing.StationId = updated.StationId;
            await _repo.UpdateAsync(id, existing);
            return existing;
        }

        public async Task CancelAsync(string id)
        {
            var existing = await _repo.GetByIdAsync(id) ?? throw new Exception("Booking not found");
            if (DateTime.UtcNow > existing.ReservationDate.AddHours(-12))
                throw new Exception("Cannot cancel booking less than 12 hours before reservation");
            existing.Status = "Cancelled";
            await _repo.UpdateAsync(id, existing);
        }

        public async Task<Booking> ApproveAsync(string id)
        {
            var existing = await _repo.GetByIdAsync(id) ?? throw new Exception("Booking not found");
            // in production: decrement station availableSlots etc.
            existing.Status = "Approved";
            existing.QrBase64 = _qr.GenerateQrBase64($"booking:{existing.Id}|owner:{existing.OwnerNIC}|station:{existing.StationId}|date:{existing.ReservationDate:o}");
            await _repo.UpdateAsync(id, existing);
            return existing;
        }

        public async Task DeleteAsync(string id)
        {
            var existing = await _repo.GetByIdAsync(id) ?? throw new Exception("Booking not found");
            // Allow deletion if booking is Cancelled or Pending
            if (existing.Status == "Approved")
            {
                throw new Exception("Cannot delete approved bookings. Please cancel first.");
            }
            await _repo.DeleteAsync(id);
        }
    }
}
